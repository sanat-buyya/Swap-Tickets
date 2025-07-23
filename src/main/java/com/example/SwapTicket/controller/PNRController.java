package com.example.SwapTicket.controller;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.example.SwapTicket.model.AdminConfig;
import com.example.SwapTicket.model.PNR;
import com.example.SwapTicket.model.Passenger;
import com.example.SwapTicket.model.TransactionHistory;
import com.example.SwapTicket.model.TransactionType;
import com.example.SwapTicket.model.User;
import com.example.SwapTicket.model.Wallet;
import com.example.SwapTicket.repository.AdminConfigRepository;
import com.example.SwapTicket.repository.PNRRepository;
import com.example.SwapTicket.repository.PassengerRepository;
import com.example.SwapTicket.repository.TransactionHistoryRepository;
import com.example.SwapTicket.repository.UserRepository;
import com.example.SwapTicket.repository.WalletRepository;
import com.example.SwapTicket.helper.EmailSender;
import org.springframework.format.annotation.DateTimeFormat;

import jakarta.servlet.http.HttpSession;

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
    private UserRepository userRepository;
    
    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private Cloudinary cloudinary;
    
    @Autowired
    private TransactionHistoryRepository transactionHistoryRepository;
    
    @Autowired
    private EmailSender emailSender;
    
    @Autowired
    private AdminConfigRepository adminConfigRepository;
    
    @Autowired
	com.example.SwapTicket.helper.RazorPayHelper razorPayHelper;
	
    @Value("${razor-pay.api.key}")
	String key;
	
	@Value("${admin.email}")
	String adminEmail;

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
    	String userEmail = (String) session.getAttribute("loggedInUserEmail");
        if (userEmail == null) {
            return "redirect:/login";
        }
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
            Map uploadResult = cloudinary.uploader().upload(ticketImage.getBytes(), ObjectUtils.emptyMap());
            pnr.setTicketImagePath((String) uploadResult.get("secure_url"));
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
        pnrRepository.save(pnr);

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
            HttpSession session,Model model) {
    	String userEmail = (String) session.getAttribute("loggedInUserEmail");
        if (userEmail == null) {
            return "redirect:/login";
        }
        List<PNR> pnrs;
        
        if (trainNumber != null && !trainNumber.isEmpty()) {
            pnrs = pnrRepository.findByTrainNumber(trainNumber);
        } else if (fromStation != null && toStation != null && !fromStation.isEmpty() && !toStation.isEmpty()) {
            // Try multiple approaches for station matching
            pnrs = findPNRsByStationsFlexible(fromStation, toStation);
        } else if (journeyDate != null) {
            pnrs = pnrRepository.findByJourneyDate(journeyDate);
        } else {
            pnrs = pnrRepository.findAllAvailableTickets(); 
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
        double adminFee = configOpt.map(AdminConfig::getBookingFee).orElse(0.0);
        
        double extraFee = adminConfigRepository.findById(1L)
                .map(AdminConfig::getExtraFee)
                .orElse(0.0);
        
        model.addAttribute("adminFee", adminFee);
        model.addAttribute("extraFee", extraFee);
        model.addAttribute("pnrs", pnrs);
        model.addAttribute("filter", filter != null ? filter : "available");
        model.addAttribute("fromStation", fromStation);
        model.addAttribute("toStation", toStation);
        model.addAttribute("trainNumber", trainNumber);
        model.addAttribute("gender", gender);
        
        return "buyPNRTickets";
    }

    private List<PNR> findPNRsByStationsFlexible(String fromStation, String toStation) {
        // Extract station information
        StationInfo fromInfo = extractStationInfo(fromStation);
        StationInfo toInfo = extractStationInfo(toStation);
        
        // Try the most specific match first
        List<PNR> results = pnrRepository.findByStationCodesAndNames(
            fromInfo.code, fromInfo.name, toInfo.code, toInfo.name);
        
        if (!results.isEmpty()) {
            return results;
        }
        
        // Try the flexible containing match
        results = pnrRepository.findByFromStationContainingIgnoreCaseAndToStationContainingIgnoreCase(
            fromInfo.searchTerm, toInfo.searchTerm);
        
        if (!results.isEmpty()) {
            return results;
        }
        
        // Fallback to original method
        return pnrRepository.findByFromStationLikeAndToStationLike(fromStation, toStation);
    }

    private static class StationInfo {
        String code = "";
        String name = "";
        String searchTerm = "";
        
        StationInfo(String code, String name, String searchTerm) {
            this.code = code != null ? code : "";
            this.name = name != null ? name : "";
            this.searchTerm = searchTerm != null ? searchTerm : "";
        }
    }

    private StationInfo extractStationInfo(String stationInput) {
        if (stationInput == null || stationInput.trim().isEmpty()) {
            return new StationInfo("", "", "");
        }
        
        String station = stationInput.trim();
        String code = "";
        String name = "";
        
        // Handle format: "Station Name (CODE)"
        if (station.contains("(") && station.contains(")")) {
            name = station.substring(0, station.indexOf("(")).trim();
            code = station.substring(station.indexOf("(") + 1, station.indexOf(")")).trim();
            return new StationInfo(code, name, name + " " + code);
        }
        
        // Handle format: "CODE - Station Name"
        if (station.contains(" - ")) {
            String[] parts = station.split(" - ", 2);
            if (parts[0].length() <= 5 && parts[0].toUpperCase().equals(parts[0])) {
                // First part looks like a code
                code = parts[0].trim();
                name = parts[1].trim();
            } else {
                // First part looks like a name
                name = parts[0].trim();
                code = parts[1].trim();
            }
            return new StationInfo(code, name, name + " " + code);
        }
        
        // Plain station name or code
        if (station.length() <= 5 && station.toUpperCase().equals(station)) {
            // Looks like a code
            return new StationInfo(station, "", station);
        } else {
            // Looks like a name
            return new StationInfo("", station, station);
        }
    }

    @GetMapping("/preview-amount")
    public String previewAmount(@RequestParam("passengerId") Long passengerId, HttpSession session, Model model) {
    	String email = (String) session.getAttribute("loggedInUserEmail");
        if (email == null) {
            return "redirect:/login";
        }
        
        double extraFee = adminConfigRepository.findById(1L)
                .map(AdminConfig::getExtraFee)
                .orElse(0.0);
        
        Passenger passenger = passengerRepository.getPassengerById(passengerId);
        double basePrice = passenger.getPrice();
        double extraBasePrice = basePrice +extraFee;

        // Fetch bookingFee and referralDiscountAmount using Optional
        double handlingCharge = adminConfigRepository.findById(1L)
            .map(AdminConfig::getBookingFee)
            .orElse(0.0);

        double referralDiscountAmount = adminConfigRepository.findById(1L)
            .map(AdminConfig::getReferralDiscountAmount)
            .orElse(0.0);
        
        double gst = basePrice * 0.18;

        String loggedInUserEmail = (String) session.getAttribute("loggedInUserEmail");
        User user = userRepository.findByEmail(loggedInUserEmail);

        // 1. Get all users referred by this user
        List<User> referredUsers = userRepository.findAllByReferredBy(user.getReferralCode());

        // 2. Count how many of those referred users made at least one purchase
        int successfulReferrals = 0;
        for (User referredUser : referredUsers) {
            boolean hasPurchased = passengerRepository.existsByBuyerEmailAndSoldTrue(referredUser.getEmail());
            if (hasPurchased) {
                successfulReferrals++;
            }
        }

        // 3. Determine next applicable milestone (5, 10, 20, 50)
        int[] milestones = {5, 10, 20, 50};
        int currentMilestone = 0;
        boolean eligibleForDiscount = false;
        int referralsLeft = 0;

        for (int m : milestones) {
            if (successfulReferrals >= m && !user.getUsedReferralDiscounts().contains(m)) {
                currentMilestone = m;
                eligibleForDiscount = true;
                break; // only one discount at a time
            }
        }

        // 4. Calculate referrals left for next milestone if not eligible
        if (!eligibleForDiscount) {
            for (int m : milestones) {
                if (successfulReferrals < m && !user.getUsedReferralDiscounts().contains(m)) {
                    referralsLeft = m - successfulReferrals;
                    break;
                }
            }
        }

        double discount = eligibleForDiscount ? referralDiscountAmount : 0.0;
        double totalAmount = basePrice + handlingCharge + gst + extraFee;

        // Add attributes to model
        model.addAttribute("passenger", passenger);
        model.addAttribute("extraFee", extraFee);
        model.addAttribute("extraBasePrice", extraBasePrice);
        model.addAttribute("basePrice", basePrice);
        model.addAttribute("referralDiscountAmount", referralDiscountAmount);
        model.addAttribute("handlingCharge", handlingCharge);
        model.addAttribute("gst", gst);
        model.addAttribute("discount", discount);
        model.addAttribute("totalAmount", totalAmount);
        model.addAttribute("successfulReferrals", successfulReferrals);
        model.addAttribute("eligibleForDiscount", eligibleForDiscount);
        model.addAttribute("currentMilestone", currentMilestone);
        model.addAttribute("referralsLeft", referralsLeft);
        model.addAttribute("key", key);

        return "previewAmount";
    }

    public int getEligibleReferralDiscount(User user) {
        String referralCode = user.getReferralCode();
        List<User> referredUsers = userRepository.findAllByReferredBy(referralCode);

        // Filter to only those who bought tickets
        long successfulReferrals = referredUsers.stream()
        	    .filter(u -> passengerRepository.existsByBuyerEmailAndSoldTrue(u.getEmail()))
        	    .count();

        List<Integer> milestones = List.of(5, 10, 20, 50);
        for (int milestone : milestones) {
            if (successfulReferrals >= milestone && !user.getUsedReferralDiscounts().contains(milestone)) {
                return milestone; // Eligible for ₹100 off
            }
        }

        return 0; // Not eligible
    }

 
    @PostMapping("/buy/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> buyTicket(@PathVariable Long id, HttpSession session) {
        // ✅ Fix: correct session key
        String buyerEmail = (String) session.getAttribute("loggedInUserEmail");

        Optional<Passenger> passengerOpt = passengerRepository.findById(id);
        Optional<Wallet> adminWalletOpt = walletRepository.findByEmail(adminEmail);

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
        
        double amountToPay = passenger.getPrice(); 

        String razorpayOrderId = razorPayHelper.createPayment(amountToPay);
        if (razorpayOrderId == null) {
            return ResponseEntity.status(500).body(Map.of("error", "Failed to create Razorpay order"));
        }

        Map<String, Object> response = new HashMap<>();
        response.put("orderId", razorpayOrderId);
        response.put("amount", (int)(amountToPay * 100));
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
            @RequestParam(value = "discount_applied", defaultValue = "false") boolean discountApplied,
            @RequestParam(value = "discount_amount", defaultValue = "0") double discountAmount,
            @RequestParam(value = "tip_amount", defaultValue = "0") double tipAmount,
            @RequestParam(value = "tip_type", defaultValue = "none") String tipType,
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

        // Calculate amounts exactly like in previewAmount
        double extraFee = adminConfigRepository.findById(1L)
                .map(AdminConfig::getExtraFee)
                .orElse(0.0);

        double basePrice = passenger.getPrice();
        double extraBasePrice= basePrice+extraFee;
        double handlingCharge = adminConfigRepository.findById(1L)
                .map(AdminConfig::getBookingFee)
                .orElse(0.0);

        double referralDiscountAmount = adminConfigRepository.findById(1L)
                .map(AdminConfig::getReferralDiscountAmount)
                .orElse(0.0);

        
        double gst = basePrice * 0.18;

        passenger.setAdminFee(handlingCharge);

        // ----- REFERRAL DISCOUNT LOGIC -----
        double discount = 0.0;
        User user = userRepository.findByEmail(buyerEmail);

        if (discountApplied) {
            int[] milestones = {5, 10, 20, 50};
            List<User> referredUsers = userRepository.findAllByReferredBy(user.getReferralCode());
            
            int successfulReferrals = 0;
            for (User referredUser : referredUsers) {
                if (passengerRepository.existsByBuyerEmailAndSoldTrue(referredUser.getEmail())) {
                    successfulReferrals++;
                }
            }

            for (int m : milestones) {
                if (successfulReferrals >= m && !user.getUsedReferralDiscounts().contains(m)) {
                    discount = referralDiscountAmount;
                    user.getUsedReferralDiscounts().add(m);
                    userRepository.save(user);
                    break; // Only apply one milestone
                }
            }
        }

        // Calculate total amount exactly like in previewAmount
        double totalAmount = extraBasePrice + handlingCharge + gst - discount + tipAmount;
        
       
        // ----- UPDATE ADMIN WALLET -----
        Optional<Wallet> adminWalletOpt = walletRepository.findByEmail(adminEmail);
        Wallet adminWallet = adminWalletOpt.orElseGet(() -> {
            Wallet newWallet = new Wallet();
            newWallet.setEmail(adminEmail);
            newWallet.setBalance(10000.0);
            return newWallet;
        });

        adminWallet.setBalance(adminWallet.getBalance() + totalAmount);
        walletRepository.save(adminWallet);
        passenger.setBuyerPaid(totalAmount);

        // Save passenger
        passengerRepository.save(passenger);

        // Notify seller via email
        User seller = userRepository.findByEmail(pnr.getSellerEmail());
        emailSender.sendSoldMessage(seller, pnr, List.of(passenger));

        // Log transaction
        TransactionHistory transaction = new TransactionHistory();
        transaction.setUserEmail(buyerEmail);
        transaction.setAmount(totalAmount);
        transaction.setType(TransactionType.DEBITED);
        transaction.setDate(LocalDate.now());
        transactionHistoryRepository.save(transaction);

        redirectAttributes.addFlashAttribute("successMessage", "🎉 Payment successful. Ticket booked!");
        return "redirect:/pnr/my-purchases";
    }

    @GetMapping("/my-purchases")
    public String viewMyPurchasedTickets(HttpSession session, Model model) {
        String currentUserEmail = (String) session.getAttribute("loggedInUserEmail");

        List<Passenger> purchasedPassengers = passengerRepository.findByBuyerEmail(currentUserEmail);
        
        model.addAttribute("purchasedPassengers", purchasedPassengers);
       
        return "myPurchasedTickets1";
    }

        @GetMapping("/external")
        public String showExternalPNRPage(Model model, HttpSession session) {
            return "pnr-enquiry-external";
        }
   



    

}
