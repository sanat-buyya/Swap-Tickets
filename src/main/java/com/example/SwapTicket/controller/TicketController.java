package com.example.SwapTicket.controller;

import com.cloudinary.Cloudinary;
import com.example.SwapTicket.model.Ticket;
import com.example.SwapTicket.model.User;
import com.example.SwapTicket.model.Wallet;
import com.example.SwapTicket.repository.TicketRepository;
import com.example.SwapTicket.repository.UserRepository;
import com.example.SwapTicket.repository.WalletRepository;
import com.example.SwapTicket.service.FileStorageService;
import com.example.SwapTicket.service.PaymentService;

import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import org.json.JSONObject;

import com.cloudinary.utils.ObjectUtils;

import java.io.IOException;
import java.util.Map;

import org.springframework.web.multipart.MultipartFile;

@Controller
@RequestMapping("/tickets")
public class TicketController {

    @Autowired
    private TicketRepository ticketRepository;
    
    @Autowired
    private PaymentService paymentService;

    @Autowired
    private FileStorageService fileStorageService;
    
    @Autowired
    private WalletRepository walletRepository;
    
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private HttpSession session;
    
    @Autowired
    private Cloudinary cloudinary;

    @PostMapping("/sell")
    public String sellTicket(
            @RequestParam("trainNumber") String trainNumber,
            @RequestParam("trainName") String trainName,
            @RequestParam("fromStation") String fromStation,
            @RequestParam("toStation") String toStation,
            @RequestParam("dateOfJourney") String dateOfJourney,
            @RequestParam("classType") String classType,
            @RequestParam("seatNumber") String seatNumber,
            @RequestParam("numberOfTickets") Integer numberOfTickets,
            @RequestParam("age") Integer age,
            @RequestParam("pnrNumber") String pnrNumber,
            @RequestParam("price") Double price,
            @RequestParam("passengerNames") String passengerNames,
            @RequestParam("gender") String gender,
            @RequestParam("ticketImage") MultipartFile ticketImage,
            @RequestParam("originalDocument") MultipartFile originalDocument,
            RedirectAttributes redirectAttributes,
            Principal principal,
            HttpSession session
    ) throws IOException {
        Ticket ticket = new Ticket();
        ticket.setTrainNumber(trainNumber);
        ticket.setTrainName(trainName);
        ticket.setFromStation(fromStation);
        ticket.setToStation(toStation);
        ticket.setDateOfJourney(LocalDate.parse(dateOfJourney));
        ticket.setClassType(classType);
        ticket.setSeatNumber(seatNumber);
        ticket.setNumberOfTickets(numberOfTickets);
        ticket.setAge(age);
        ticket.setPnrNumber(pnrNumber);
        ticket.setPrice(price);
        ticket.setPassengerNames(Arrays.asList(passengerNames.split("\\s*,\\s*")));
        ticket.setGender(gender);
        ticket.setSold(false);

        String sellerEmail = (String) session.getAttribute("loggedInUserEmail");
        ticket.setSellerEmail(sellerEmail);

        // 💼 Create seller wallet if not already exists
        Optional<Wallet> existingWallet = walletRepository.findByEmail(sellerEmail);
        if 	(existingWallet.isEmpty()) {
            Wallet wallet = new Wallet();
            wallet.setEmail(sellerEmail);
            wallet.setBalance(0.0);
            walletRepository.save(wallet);
        }
        // 📄 Save ticket image
     // 📄 Save ticket image
        if (!ticketImage.isEmpty()) {
            Map uploadResultImage = cloudinary.uploader().upload(ticketImage.getBytes(), ObjectUtils.asMap(
                "resource_type", "auto" // Automatically detects if it's JPG or PDF
            ));
            ticket.setTicketImagePath((String) uploadResultImage.get("secure_url"));
        }

        // 📑 Save original document (PDF or other)
        if (!originalDocument.isEmpty()) {
            Map uploadResultImage = cloudinary.uploader().upload(originalDocument.getBytes(), ObjectUtils.asMap(
                "resource_type", "auto" // Automatically detects if it's JPG or PDF
            ));
            ticket.setOriginalDocumentPath((String) uploadResultImage.get("secure_url"));
        }

        ticketRepository.save(ticket);
        redirectAttributes.addFlashAttribute("successMessage", "🎉 Ticket submitted successfully! Go to My Listed Tickets to check if it is sold.");
        return "redirect:/user/home";

          
    }


    @GetMapping("/buy")
    public String showFilteredTickets(
            @RequestParam(required = false) String filter,
            @RequestParam(required = false) String fromStation,
            @RequestParam(required = false) String toStation,
            @RequestParam(required = false) String pnr,
            Model model) {

        List<Ticket> tickets;
        boolean sold = "sold".equalsIgnoreCase(filter);

        if (pnr != null && !pnr.isEmpty()) {
            tickets = ticketRepository.findByPnrNumberAndSold(pnr, sold);
        } else if (fromStation != null && toStation != null && !fromStation.isEmpty() && !toStation.isEmpty()) {
            tickets = ticketRepository.findByFromStationIgnoreCaseAndToStationIgnoreCaseAndSold(fromStation, toStation, sold);
        } else {
            tickets = ticketRepository.findBySold(sold);
        }

        model.addAttribute("tickets", tickets);
        model.addAttribute("filter", sold ? "sold" : "available");
        model.addAttribute("fromStation", fromStation);
        model.addAttribute("toStation", toStation);
        model.addAttribute("pnr", pnr);
        return "buyTickets";
    }
    
    @PostMapping("/buy/{id}")
    public String buyTicket(@PathVariable Long id, HttpSession session, RedirectAttributes redirectAttributes) {
        String buyerEmail = (String) session.getAttribute("email");

        Optional<Ticket> ticketOpt = ticketRepository.findById(id);
        Optional<Wallet> buyerWalletOpt = walletRepository.findByEmail(buyerEmail);
        Optional<Wallet> adminWalletOpt = walletRepository.findByEmail("admin@swapticket.com");

        if (ticketOpt.isEmpty() || buyerWalletOpt.isEmpty() || adminWalletOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Transaction failed: Missing ticket or wallet.");
            return "redirect:/tickets/buy";
        }

        Ticket ticket = ticketOpt.get();

        // 🚫 Prevent seller from buying their own ticket
        if (buyerEmail != null && buyerEmail.equals(ticket.getSellerEmail())) {
            redirectAttributes.addFlashAttribute("error", "❌ You cannot buy your own ticket.");
            return "redirect:/tickets/buy";
        }

        Wallet buyerWallet = buyerWalletOpt.get();
        Wallet adminWallet = adminWalletOpt.get();

        double price = ticket.getPrice();

        if (ticket.isSold()) {
            redirectAttributes.addFlashAttribute("error", "Ticket already sold.");
            return "redirect:/tickets/buy";
        }

        if (buyerWallet.getBalance() < price) {
            redirectAttributes.addFlashAttribute("error", "Insufficient balance.");
            return "redirect:/tickets/buy";
        }

        // Perform transaction
        buyerWallet.setBalance(buyerWallet.getBalance() - price);
        adminWallet.setBalance(adminWallet.getBalance() + price);

        ticket.setSold(true);
        ticket.setBuyerEmail(buyerEmail);

        walletRepository.save(buyerWallet);
        walletRepository.save(adminWallet);
        ticketRepository.save(ticket);

        redirectAttributes.addFlashAttribute("successMessage", "🎉 Ticket bought successfully!");

        return "redirect:/tickets/buy";
    }


    @GetMapping("/my/{id}")
    public String viewMyTicket(@PathVariable Long id, Model model) {
        Optional<Ticket> ticketOptional = ticketRepository.findById(id);
        if (ticketOptional.isPresent()) {
            model.addAttribute("ticket", ticketOptional.get());
            return "myTicket";
        } else {
            return "redirect:/tickets/buy";
        }
    }
    
    @GetMapping("/my-tickets")
    public String showMyTickets(Model model, HttpSession session) {
        String email = (String) session.getAttribute("loggedInUserEmail");
        if (email == null) {
            return "redirect:/login";
        }

        List<Ticket> myTickets = ticketRepository.findBySellerEmail(email);
        System.out.println("Number of tickets: " + myTickets.size()); 
        model.addAttribute("tickets", myTickets);
        return "myTickets";
    }
    @GetMapping("/my-purchases")
    public String viewMyPurchasedTickets(HttpSession session, Model model) {
        String currentUserEmail = (String) session.getAttribute("loggedInUserEmail");

        List<Ticket> purchasedTickets = ticketRepository.findByBuyerEmail(currentUserEmail);
        System.out.println("Purchased Tickets for " + currentUserEmail + ": " + purchasedTickets.size());

        model.addAttribute("tickets", purchasedTickets);
        return "myPurchasedTickets";
    }
    
    @GetMapping("/tickets/confirm/{id}")
    public String confirmPayment(@PathVariable Long id,
                                 @RequestParam("paymentId") String paymentId,
                                 HttpSession session,
                                 RedirectAttributes redirectAttributes) {
        String buyerEmail = (String) session.getAttribute("email");
        Optional<Ticket> ticketOpt = ticketRepository.findById(id);
        Optional<Wallet> adminWalletOpt = walletRepository.findByEmail("admin@swapticket.com");

        if (ticketOpt.isEmpty() || adminWalletOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Payment failed or ticket not found.");
            return "redirect:/tickets/buy";
        }

        Ticket ticket = ticketOpt.get();
        if (ticket.isSold()) {
            redirectAttributes.addFlashAttribute("error", "Ticket already sold.");
            return "redirect:/tickets/buy";
        }

        // Update ticket and admin wallet
        ticket.setSold(true);
        ticket.setBuyerEmail(buyerEmail);
        adminWalletOpt.get().setBalance(adminWalletOpt.get().getBalance() + ticket.getPrice());

        ticketRepository.save(ticket);
        walletRepository.save(adminWalletOpt.get());

        redirectAttributes.addFlashAttribute("successMessage", "🎉 Payment successful! Ticket bought.");
        return "redirect:/tickets/buy";
    }
    @PostMapping("/tickets/initiate-payment")
    public String initiatePayment(@RequestParam("ticketId") Long ticketId, Model model, HttpSession session) {
        try {
            RazorpayClient razorpay = new RazorpayClient("razor-pay.api.key", "razor-pay.api.secret");

            // Example: set the ticket price here (in paise)
            int amount = 50000; // = ₹500.00

            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", amount);
            orderRequest.put("currency", "INR");
            orderRequest.put("payment_capture", 1);

            Order order = razorpay.orders.create(orderRequest);

            // Send order details to frontend
            model.addAttribute("orderId", order.get("id"));
            model.addAttribute("amount", amount);
            model.addAttribute("ticketId", ticketId);
            model.addAttribute("apiKey", "razor-pay.api.key"); // for JS frontend

            return "razorpayPayment"; // Razorpay payment page (you'll create this)
        } catch (RazorpayException e) {
            e.printStackTrace();
            model.addAttribute("error", "Payment failed to initiate.");
            return "errorPage";
        }
    }
    
    @GetMapping("/tickets/buy-now/{ticketId}")
    public String buyNow(@PathVariable Long ticketId, Model model, HttpSession session) {
        try {
            RazorpayClient razorpay = new RazorpayClient("razor-pay.api.key", "razor-pay.api.secret");

            Optional<Ticket> ticketOpt = ticketRepository.findById(ticketId);
            if (ticketOpt.isEmpty()) {
                model.addAttribute("error", "Ticket not found");
                return "userHome";
            }

            Ticket ticket = ticketOpt.get();

            JSONObject orderRequest = new JSONObject();
            double serviceFee = 20.0;
            double totalAmount = ticket.getPrice() + serviceFee;
            orderRequest.put("amount", (int)(totalAmount * 100)); // price + ₹20 service charge
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", "ticket_" + ticketId);
            orderRequest.put("payment_capture", true);

            Order order = razorpay.orders.create(orderRequest);

            model.addAttribute("ticket", ticket);
            model.addAttribute("razorpayOrderId", order.get("id"));
            model.addAttribute("razorpayKey", "razor-pay.api.key");
            model.addAttribute("ticketId", ticketId);
            model.addAttribute("amount", totalAmount * 100);
           return "paymentPage"; // Create paymentPage.html to handle Razorpay checkout
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", "Payment failed to initialize.");
            return "userHome";
        }
    }

    @PostMapping("/razorpay/success")
    public String paymentSuccess(@RequestParam("ticketId") Long ticketId, HttpSession session, RedirectAttributes flash) {
        String buyerEmail = (String) session.getAttribute("loggedInUserEmail");

        Optional<Ticket> ticketOpt     = ticketRepository.findById(ticketId);
        Optional<Wallet> adminWalletOpt = walletRepository.findByEmail("admin@swapticket.com");

        if (ticketOpt.isEmpty() || adminWalletOpt.isEmpty()) {
            flash.addFlashAttribute("error", "Payment error or wallet missing.");
            return "redirect:/tickets/buy";
        }

        Ticket ticket       = ticketOpt.get();
        Wallet adminWallet  = adminWalletOpt.get();

        // 1) mark sold & buyer
        ticket.setSold(true);
        ticket.setBuyerEmail(buyerEmail);
        ticketRepository.save(ticket);

        // 2) credit service fee to admin only
        double serviceFee = 20.0;
        adminWallet.setBalance(adminWallet.getBalance() + serviceFee);
        walletRepository.save(adminWallet);

        // 3) now transfer the base fare to seller + mark sellerPaid
        boolean ok = paymentService.transferToSeller(ticket);
        if (!ok) {
            flash.addFlashAttribute("error", "Failed to pay seller—please contact support.");
        } else {
            flash.addFlashAttribute("successMessage", "🎉 Payment successful!");
        }

        return "redirect:/tickets/my-purchases";
    }


    @GetMapping("/confirm/{ticketId}")
    public String confirmTicketPurchase(@PathVariable Long ticketId,
                                        @RequestParam String paymentId,
                                        HttpSession session,
                                        Model model) {
        Optional<Ticket> optionalTicket = ticketRepository.findById(ticketId);
        if (optionalTicket.isEmpty()) {
            model.addAttribute("error", "Ticket not found");
            return "error";
        }

        Ticket ticket = optionalTicket.get();
        ticket.setSold(true);
        ticket.setBuyerEmail((String) session.getAttribute("loggedInUserEmail"));
        ticketRepository.save(ticket);

        Optional<Wallet> adminWalletOpt = walletRepository.findByEmail("admin@swapticket.com");
        if (adminWalletOpt.isPresent()) {
            Wallet adminWallet = adminWalletOpt.get();
            adminWallet.setBalance(adminWallet.getBalance() + ticket.getPrice());
            walletRepository.save(adminWallet);
        }

        return "redirect:/tickets/my-purchases";
    }
    







}
