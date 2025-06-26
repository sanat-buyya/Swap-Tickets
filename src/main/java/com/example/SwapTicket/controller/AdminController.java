package com.example.SwapTicket.controller;

import com.example.SwapTicket.helper.EmailSender;
import com.example.SwapTicket.model.AdminConfig;
import com.example.SwapTicket.model.Passenger;
import com.example.SwapTicket.model.SupportMessage;
import com.example.SwapTicket.model.TransactionHistory;
import com.example.SwapTicket.model.TransactionType;
import com.example.SwapTicket.model.User;
import com.example.SwapTicket.model.Wallet;
import com.example.SwapTicket.repository.AdminConfigRepository;
import com.example.SwapTicket.repository.PassengerRepository;
import com.example.SwapTicket.repository.SupportMessageRepository;
import com.example.SwapTicket.repository.TransactionHistoryRepository;
import com.example.SwapTicket.repository.UserRepository;
import com.example.SwapTicket.repository.WalletRepository;
import com.example.SwapTicket.service.PaymentService;
import com.example.SwapTicket.service.UserService;

import org.springframework.web.servlet.mvc.support.RedirectAttributes;


import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;


    @Controller
    @RequestMapping("/admin")
    public class AdminController {

        @Autowired
        private PassengerRepository passengerRepository;

        @Autowired
        private WalletRepository walletRepository;

        @Autowired
        private PaymentService paymentService;

        @Autowired
        private UserService userService;
        
        @Autowired
        private UserRepository userRepository;
        
        @Autowired
        private TransactionHistoryRepository transactionHistoryRepository;

        @Autowired
        private SupportMessageRepository supportRepo;
        
        @Autowired
        private EmailSender emailSender;
        
        @Autowired
        private AdminConfigRepository adminConfigRepository;
        
        @Value("${admin.email}")
    	String ADMIN_EMAIL;
       
        @GetMapping("/dashboard")
        public String adminDashboard(Model model, HttpSession session) {
            if (session.getAttribute("admin") == null) {
                return "redirect:/login";
            }

            List<Passenger> passengers = passengerRepository.findBySoldTrueAndSellerPaidFalse();
            Wallet adminWallet = walletRepository.findByEmail(ADMIN_EMAIL).orElse(new Wallet());

            model.addAttribute("passengers", passengers);
            model.addAttribute("adminWallet", adminWallet.getBalance());

            return "adminDashboard";
        }

        @PostMapping("/pay-seller/{passengerId}")
        public String paySeller(@PathVariable Long passengerId, RedirectAttributes redirectAttributes, HttpSession session) {
            if (session.getAttribute("admin") == null) {
                return "redirect:/login";
            }

            Optional<Passenger> optionalPassenger = passengerRepository.findById(passengerId);
            if (optionalPassenger.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Passenger not found");
                return "redirect:/admin/dashboard";
            }

            Passenger passenger = optionalPassenger.get();
            if (!passenger.isSold()) {
                redirectAttributes.addFlashAttribute("error", "Ticket is not sold yet");
                return "redirect:/admin/dashboard";
            }

            boolean success = paymentService.paySeller(passenger);
            if (!success) {
                redirectAttributes.addFlashAttribute("error", "Payment failed: Insufficient balance or wallet not found");
                return "redirect:/admin/dashboard";
            }

            // Record the credited transaction for the seller
            TransactionHistory transaction = new TransactionHistory();
            transaction.setUserEmail(passenger.getSellerEmail());
            transaction.setAmount(passenger.getPrice());
            transaction.setType(TransactionType.CREDITED);
            transaction.setDate(LocalDate.now());

            transactionHistoryRepository.save(transaction);
            
            User seller = userRepository.findByEmail(passenger.getSellerEmail());
            if (seller != null) {
                emailSender.sendPayoutMessage(seller, passenger);
            }
            
            redirectAttributes.addFlashAttribute("success", "Seller paid successfully!");
            return "redirect:/admin/dashboard";
        }
        
        @PostMapping("/cancel-payment/{passengerId}")
        public String cancelPayment(@PathVariable Long passengerId, RedirectAttributes redirectAttributes, HttpSession session) {
            if (session.getAttribute("admin") == null) {
                return "redirect:/login";
            }

            Optional<Passenger> optionalPassenger = passengerRepository.findById(passengerId);
            if (optionalPassenger.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Passenger not found");
                return "redirect:/admin/dashboard";
            }

            Passenger passenger = optionalPassenger.get();

            // Save cancelled transaction
            TransactionHistory cancelledTxn = new TransactionHistory();
            cancelledTxn.setUserEmail(passenger.getSellerEmail());
            cancelledTxn.setAmount(passenger.getPrice());
            cancelledTxn.setType(TransactionType.Cancelled);
            cancelledTxn.setDate(LocalDate.now());

            transactionHistoryRepository.save(cancelledTxn);

            passenger.setSellerPaid(true);
            passengerRepository.save(passenger);

            redirectAttributes.addFlashAttribute("success", "Payment marked as cancelled.");
            return "redirect:/admin/dashboard";
        }


        @GetMapping("/fee")
        public String viewFeeSetting(Model model) {
            AdminConfig config = adminConfigRepository.findById(1L)
                .orElseGet(() -> {
                    AdminConfig newConfig = new AdminConfig();
                    newConfig.setBookingFee(20); // default
                    newConfig.setReferralDiscountAmount(100.0); // default
                    newConfig.setExtraFee(10.0); // default
                    return adminConfigRepository.save(newConfig);
                });

            model.addAttribute("bookingFee", config.getBookingFee());
            model.addAttribute("referralDiscount", config.getReferralDiscountAmount());
            model.addAttribute("extraFee", config.getExtraFee());
            return "adminFee";
        }

        @PostMapping("/fee")
        public String updateFee(@RequestParam("fee") double fee,
                                @RequestParam("referralDiscount") double referralDiscount,
                                @RequestParam("extraFee") double extraFee) {
            AdminConfig config = adminConfigRepository.findById(1L)
                .orElseThrow(() -> new RuntimeException("Admin config not found"));
            
            config.setBookingFee(fee);
            config.setReferralDiscountAmount(referralDiscount);
            config.setExtraFee(extraFee);
            adminConfigRepository.save(config);
            return "redirect:/admin/fee?success";
        }
        
        @GetMapping("/view-tickets")
        public String viewTickets(Model model) {
            long totalTickets = passengerRepository.count();
            long soldTickets = passengerRepository.countBySoldTrue();
            long availableTickets = passengerRepository.countBySoldFalse();

            model.addAttribute("totalTickets", totalTickets);
            model.addAttribute("soldTickets", soldTickets);
            model.addAttribute("availableTickets", availableTickets);
            model.addAttribute("passengers", passengerRepository.findAll());
            
            return "adminViewTickets";
        }
        
        @GetMapping("/users")
        public String viewAllUsers(
                @RequestParam(defaultValue = "0") int page,
                @RequestParam(defaultValue = "50") int size,
                Model model) {

            Page<User> userPage = userRepository.findAll(PageRequest.of(page, size));

            List<Map<String, Object>> userData = new ArrayList<>();
            for (User user : userPage.getContent()) {
                Map<String, Object> data = new HashMap<>();
                data.put("user", user);

                walletRepository.findByEmail(user.getEmail())
                    .ifPresent(wallet -> data.put("walletBalance", wallet.getBalance()));

                int referralCount = userRepository.findAllByReferredBy(user.getReferralCode()).size();
                data.put("referralCount", referralCount);

                // 👇 Add status to display block/unblock buttons
                data.put("status", user.getStatus());
                data.put("blockUrl", "/admin/blockUser/" + user.getId());
                data.put("unblockUrl", "/admin/unblockUser/" + user.getId());

                userData.add(data);
            }

            model.addAttribute("userData", userData);
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", userPage.getTotalPages());
            model.addAttribute("totalUsers", userPage.getTotalElements());

            return "adminViewUsers";
        }
        
        @GetMapping("/blockUser/{id}")
        public String blockUser(@PathVariable Long id) {
            User user = userService.findById(id);
            if (user != null) {
                user.setStatus("BLOCKED");
                userService.saveUser(user);
            }
            return "redirect:/admin/users";
        }

        @GetMapping("/unblockUser/{id}")
        public String unblockUser(@PathVariable Long id) {
            User user = userService.findById(id);
            if (user != null) {
                user.setStatus("ACTIVE");
                userService.saveUser(user);
            }
            return "redirect:/admin/users";
        }


        @GetMapping("/transactions")
        public String showTransactions(Model model,
                @RequestParam(defaultValue = "0") int page,
                @RequestParam(defaultValue = "50") int size) {
            List<Passenger> soldPassengers = passengerRepository.findBySoldTrue();

            AdminConfig config = adminConfigRepository.findById(1L).orElse(null);
            double adminFee = config != null ? config.getBookingFee() : 20; // default fallback

            double totalRevenue = soldPassengers.stream()
            	    .mapToDouble(Passenger::getAdminFee) // assuming this exists
            	    .sum();

            Pageable pageable = PageRequest.of(page, size);
            Page<Passenger> pageResult = passengerRepository.findBySoldTrue(pageable);
            
            model.addAttribute("transactions", soldPassengers);
            model.addAttribute("totalRevenue", totalRevenue);
            model.addAttribute("adminFee", adminFee);
            model.addAttribute("transactions", pageResult.getContent());
            model.addAttribute("totalPages", pageResult.getTotalPages());
            model.addAttribute("currentPage", page);
            return "adminTransactions";
        }

        @GetMapping("/support/messages")
        public String viewSupportUserList(Model model) {
            List<String> userEmails = supportRepo.findDistinctUserEmails();
            model.addAttribute("userEmails", userEmails);
            return "adminSupportUserList"; 
        }

        @GetMapping("/support/chat")
        public String viewUserChat(@RequestParam String email, Model model) {
            List<SupportMessage> messages = supportRepo.findByUserEmailOrderByTimestampAsc(email);
            model.addAttribute("messages", messages);
            model.addAttribute("userEmail", email);
            return "adminUserChat"; 
        }

        @PostMapping("/support/reply")
        public String replyToSupport(@RequestParam String reply,
                                     @RequestParam String email) {
            SupportMessage msg = new SupportMessage();
            msg.setUserEmail(email);
            msg.setAdminReply(reply);
            msg.setResolved(true);
            msg.setTimestamp(LocalDateTime.now());
            supportRepo.save(msg);
            return "redirect:/admin/support/chat?email=" + email;
        }
        
        @Transactional
        @GetMapping("/support/close")
        public String closeConversation(@RequestParam String email) {
            supportRepo.deleteByUserEmail(email);
            return "redirect:/admin/support/messages"; // or wherever you want to redirect
        }




}