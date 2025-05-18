package com.example.SwapTicket.controller;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.example.SwapTicket.model.AdminConfig;
import com.example.SwapTicket.model.PNR;
import com.example.SwapTicket.model.Passenger;
import com.example.SwapTicket.model.TransactionHistory;
import com.example.SwapTicket.model.TransactionType;
import com.example.SwapTicket.model.Wallet;
import com.example.SwapTicket.repository.AdminConfigRepository;
import com.example.SwapTicket.repository.PNRRepository;
import com.example.SwapTicket.repository.PassengerRepository;
import com.example.SwapTicket.repository.TransactionHistoryRepository;
import com.example.SwapTicket.repository.WalletRepository;
import com.example.SwapTicket.helper.RazorPayHelper;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import org.springframework.format.annotation.DateTimeFormat;


import jakarta.servlet.http.HttpSession;

import org.cloudinary.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.time.LocalDate;
import java.util.*;

@Controller
@RequestMapping("/pnr")
public class PNRController {

    @Autowired
    private PNRRepository pnrRepository;

    @Autowired
    private PassengerRepository passengerRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private Cloudinary cloudinary;
    
    @Autowired
    private TransactionHistoryRepository transactionHistoryRepository;

    
    @Autowired
    private AdminConfigRepository adminConfigRepository;
    
    @Autowired
	com.example.SwapTicket.helper.RazorPayHelper razorPayHelper;
	@Value("${razor-pay.api.key}")
	String key;

    @PostMapping("/sell")
    public String sellPNRTicket(
            @RequestParam("pnrNumber") String pnrNumber,
            @RequestParam("trainNumber") String trainNumber,
            @RequestParam("fromStation") String fromStation,
            @RequestParam("toStation") String toStation,
            @RequestParam("journeyDate") String journeyDate,
            @RequestParam("numberOfTickets") Integer numberOfTickets,
            @RequestParam("upiId") String upiId,
            @RequestParam("sellerMobile") String sellerMobile,
            @RequestParam("ticketImage") MultipartFile ticketImage,
            HttpSession session,
            @RequestParam Map<String, String> params,
            @RequestParam("documentUrls") List<MultipartFile> documentFiles,
            RedirectAttributes redirectAttributes
    ) throws IOException {

        if (pnrRepository.existsById(pnrNumber)) {
            redirectAttributes.addFlashAttribute("error", "PNR already exists.");
            return "redirect:/user/home";
        }

        String sellerEmail = (String) session.getAttribute("loggedInUserEmail");

        PNR pnr = new PNR();
        pnr.setPnrNumber(pnrNumber);
        pnr.setTrainNumber(trainNumber);
        pnr.setFromStation(fromStation);
        pnr.setToStation(toStation);
        pnr.setJourneyDate(LocalDate.parse(journeyDate));
        pnr.setNumberOfTickets(numberOfTickets);
        pnr.setUpiId(upiId);
        pnr.setSellerMobile(sellerMobile);
        pnr.setSellerEmail(sellerEmail);

        // Upload ticket image
        if (!ticketImage.isEmpty()) {
            Map imageResult = cloudinary.uploader().upload(ticketImage.getBytes(), ObjectUtils.emptyMap());
            pnr.setTicketImagePath((String) imageResult.get("secure_url"));
        }

        List<Passenger> passengerList = new ArrayList<>();

        for (int i = 0; i < numberOfTickets; i++) {
            Passenger p = new Passenger();

            p.setName(params.get("passengerNames[" + i + "]"));
            p.setGender(params.get("genders[" + i + "]"));
            p.setSeat(params.get("seats[" + i + "]"));
            
            // Defensive parsing for age
            String ageStr = params.get("ages[" + i + "]");
            p.setAge(ageStr != null && !ageStr.isEmpty() ? Integer.parseInt(ageStr) : 0);

            p.setCoach(params.get("coaches[" + i + "]"));

            // Defensive parsing for price
            String priceStr = params.get("prices[" + i + "]");
            p.setPrice(priceStr != null && !priceStr.isEmpty() ? Double.parseDouble(priceStr) : 0.0);

            // Document upload
            if (documentFiles.size() > i && !documentFiles.get(i).isEmpty()) {
                Map docResult = cloudinary.uploader().upload(documentFiles.get(i).getBytes(), ObjectUtils.emptyMap());
                p.setDocumentUrls((String) docResult.get("secure_url"));
            }

            p.setPnr(pnr);
            passengerList.add(p);
        }


        pnr.setPassenger(passengerList);
        pnrRepository.save(pnr); // Cascade saves all passengers

        redirectAttributes.addFlashAttribute("successMessage", "🎉 Ticket with PNR added successfully!");
        return "redirect:/user/home";
    }
   
    @GetMapping("/buy")
    public String showFilteredPNRTickets(
            @RequestParam(required = false) String filter,
            @RequestParam(required = false) String fromStation,
            @RequestParam(required = false) String toStation,
            @RequestParam(required = false) String trainNumber,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate journeyDate,
            @RequestParam(required = false) String gender,
            Model model) {

        List<PNR> pnrs;

        if (trainNumber != null && !trainNumber.isEmpty()) {
            pnrs = pnrRepository.findByTrainNumber(trainNumber);
        } else if (fromStation != null && toStation != null && !fromStation.isEmpty() && !toStation.isEmpty()) {
            pnrs = pnrRepository.findByFromStationLikeAndToStationLike(fromStation, toStation);
        } else if (journeyDate != null) {
            pnrs = pnrRepository.findByJourneyDate(journeyDate);
        } else {
            pnrs = pnrRepository.findAll();
        }

        // Remove PNRs with past journey dates
        pnrs = pnrs.stream()
                .filter(pnr -> !pnr.getJourneyDate().isBefore(LocalDate.now()))
                .toList();

        // Filter passengers based on sold/available and gender
        boolean filterSold = "sold".equalsIgnoreCase(filter);
        for (PNR pnr : pnrs) {
            List<Passenger> filteredPassengers = pnr.getPassenger().stream()
                    .filter(p -> (filter == null || p.isSold() == filterSold))
                    .filter(p -> (gender == null || gender.isEmpty() || p.getGender().equalsIgnoreCase(gender)))
                    .toList();
            pnr.setPassenger(filteredPassengers);
        }

        // Remove PNRs with no matching passengers
        pnrs = pnrs.stream()
                .filter(pnr -> !pnr.getPassenger().isEmpty())
                .toList();
        
        Optional<AdminConfig> configOpt = adminConfigRepository.findById(1L);
        double adminFee = configOpt.map(AdminConfig::getBookingFee).orElse(0.0); // fallback value
        model.addAttribute("adminFee", adminFee);
        model.addAttribute("pnrs", pnrs);
        model.addAttribute("filter", filter != null ? filter : "available");
        model.addAttribute("fromStation", fromStation);
        model.addAttribute("toStation", toStation);
        model.addAttribute("trainNumber", trainNumber);
        model.addAttribute("gender", gender);
        return "buyPNRTickets";
    }


    
    @PostMapping("/buy/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> buyTicket(@PathVariable Long id, HttpSession session) {
        // ✅ Fix: correct session key
        String buyerEmail = (String) session.getAttribute("loggedInUserEmail");

        Optional<Passenger> passengerOpt = passengerRepository.findById(id);
        Optional<Wallet> adminWalletOpt = walletRepository.findByEmail("admin@swapticket.com");

        if (passengerOpt.isEmpty() || adminWalletOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Transaction failed: Passenger or Wallet not found"));
        }

        Passenger passenger = passengerOpt.get();
        PNR pnr = passenger.getPnr();

        // ❌ Prevent self-purchase
        if (buyerEmail != null && buyerEmail.equals(pnr.getSellerEmail())) {
            return ResponseEntity.badRequest().body(Map.of("error", "❌ You cannot buy your own ticket."));
        }

        if (passenger.isSold()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Passenger ticket already sold."));
        }
        
        double adminFee = adminConfigRepository.findById(1L).orElseThrow().getBookingFee();

        double amountToPay = passenger.getPrice() + adminFee; 

        String razorpayOrderId = razorPayHelper.createPayment(amountToPay);
        if (razorpayOrderId == null) {
            return ResponseEntity.status(500).body(Map.of("error", "Failed to create Razorpay order"));
        }

        Map<String, Object> response = new HashMap<>();
        response.put("orderId", razorpayOrderId);
        response.put("amount", (int)(amountToPay * 100));  // in paisa
        response.put("currency", "INR");
        response.put("key", razorPayHelper.getKey());
        response.put("passengerId", passenger.getId());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/confirm/{passengerId}")
    public String confirmPayment(
            @PathVariable Long passengerId,
            @RequestParam("razorpay_payment_id") String razorpayPaymentId,
            @RequestParam("razorpay_order_id") String razorpayOrderId,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        String buyerEmail = (String) session.getAttribute("loggedInUserEmail");

        Optional<Passenger> passengerOpt = passengerRepository.findById(passengerId);
        if (passengerOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Passenger not found.");
            return "redirect:/pnr/buy";
        }

        Passenger passenger = passengerOpt.get();
        passenger.setBuyerEmail(buyerEmail);
        PNR pnr = passenger.getPnr();
        if (pnr != null) {
            passenger.setSellerEmail(pnr.getSellerEmail());
        } else {
            throw new RuntimeException("PNR not found for passenger ID: " + passenger.getId());
        }
        passenger.setSold(true);
        passenger.setRazorpayPaymentId(razorpayPaymentId);
        passenger.setRazorpayOrderId(razorpayOrderId);

        double ticketPrice = passenger.getPrice();
        double adminFee = adminConfigRepository.findById(1L).orElseThrow().getBookingFee();
        double totalAmount = ticketPrice + adminFee;
        passenger.setAdminFee(adminFee);

        // Admin wallet update: Add total amount
        Optional<Wallet> adminWalletOpt = walletRepository.findByEmail("admin@swapticket.com");
        Wallet adminWallet = adminWalletOpt.orElseGet(() -> {
            Wallet newWallet = new Wallet();
            newWallet.setEmail("admin@swapticket.com");
            newWallet.setBalance(0.0);
            return newWallet;
        });

        adminWallet.setBalance(adminWallet.getBalance() + totalAmount);
        walletRepository.save(adminWallet);

        // Save passenger as sold
        passengerRepository.save(passenger);

        redirectAttributes.addFlashAttribute("successMessage", "🎉 Payment successful. Ticket booked!");
        TransactionHistory transaction = new TransactionHistory();
        transaction.setUserEmail(buyerEmail);
        transaction.setAmount(totalAmount);
        transaction.setType(TransactionType.DEBITED);
        transaction.setDate(LocalDate.now());

        transactionHistoryRepository.save(transaction);

        return "redirect:/pnr/my-purchases";
    }


    @GetMapping("/my-purchases")
    public String viewMyPurchasedTickets(HttpSession session, Model model) {
        String currentUserEmail = (String) session.getAttribute("loggedInUserEmail");

        List<Passenger> purchasedPassengers = passengerRepository.findByBuyerEmail(currentUserEmail);

        model.addAttribute("purchasedPassengers", purchasedPassengers);
       
        return "myPurchasedTickets1";
    }


   



    

}
