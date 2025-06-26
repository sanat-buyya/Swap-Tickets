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
        public String adminDashboard(
                @RequestParam(value = "search", required = false) String search,
                @RequestParam(value = "filterType", required = false, defaultValue = "all") String filterType,
                Model model, 
                HttpSession session) {
            
            if (session.getAttribute("admin") == null) {
                return "redirect:/login";
            }
            
            List<Passenger> passengers;
            
            // Apply search and filter logic using repository methods
            if (search != null && !search.trim().isEmpty()) {
                String searchTerm = search.trim();
                
                switch (filterType.toLowerCase()) {
                    case "pnr":
                        passengers = passengerRepository.findBySoldTrueAndSellerPaidFalseAndPnrContaining(searchTerm);
                        break;
                    case "train":
                        passengers = passengerRepository.findBySoldTrueAndSellerPaidFalseAndTrainContaining(searchTerm);
                        break;
                    case "buyer":
                        passengers = passengerRepository.findBySoldTrueAndSellerPaidFalseAndBuyerEmailContaining(searchTerm);
                        break;
                    case "seller":
                        passengers = passengerRepository.findBySoldTrueAndSellerPaidFalseAndSellerMobileContaining(searchTerm);
                        break;
                    case "all":
                    default:
                        passengers = passengerRepository.findBySoldTrueAndSellerPaidFalseAndSearchAll(searchTerm);
                        break;
                }
            } else {
                passengers = passengerRepository.findBySoldTrueAndSellerPaidFalse();
            }
            
            Wallet adminWallet = walletRepository.findByEmail(ADMIN_EMAIL).orElse(new Wallet());
            
            model.addAttribute("passengers", passengers);
            model.addAttribute("adminWallet", adminWallet.getBalance());
            model.addAttribute("currentSearch", search);
            model.addAttribute("currentFilter", filterType);
            
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
        public String viewTickets(
                @RequestParam(defaultValue = "0") int page,
                @RequestParam(defaultValue = "50") int size,
                @RequestParam(value = "search", required = false) String search,
                @RequestParam(value = "searchType", required = false, defaultValue = "all") String searchType,
                @RequestParam(value = "status", required = false, defaultValue = "all") String status,
                Model model) {
            
            Page<Passenger> passengerPage;
            
            // Clean up search parameter
            String cleanSearch = (search != null && !search.trim().isEmpty()) ? search.trim() : null;
            String cleanStatus = (status != null && !status.equals("all")) ? status : null;
            
            // Convert status to boolean for database query
            Boolean soldStatus = null;
            if ("sold".equals(cleanStatus)) {
                soldStatus = true;
            } else if ("available".equals(cleanStatus)) {
                soldStatus = false;
            }
            
            // Apply search and status filter
            if (cleanSearch != null && soldStatus != null) {
                // Both search and status filter
                switch (searchType) {
                    case "pnr":
                        passengerPage = passengerRepository.findByPnrPnrNumberContainingIgnoreCaseAndSold(
                            cleanSearch, soldStatus, PageRequest.of(page, size));
                        break;
                    case "passenger":
                        passengerPage = passengerRepository.findByNameContainingIgnoreCaseAndSold(
                            cleanSearch, soldStatus, PageRequest.of(page, size));
                        break;
                    case "seller":
                        passengerPage = passengerRepository.findBySellerEmailContainingIgnoreCaseAndSold(
                            cleanSearch, soldStatus, PageRequest.of(page, size));
                        break;
                    default: // "all"
                        passengerPage = passengerRepository.findBySearchAllAndSold(
                            cleanSearch, soldStatus, PageRequest.of(page, size));
                        break;
                }
            } else if (cleanSearch != null) {
                // Only search filter
                switch (searchType) {
                    case "pnr":
                        passengerPage = passengerRepository.findByPnrPnrNumberContainingIgnoreCase(
                            cleanSearch, PageRequest.of(page, size));
                        break;
                    case "passenger":
                        passengerPage = passengerRepository.findByNameContainingIgnoreCase(
                            cleanSearch, PageRequest.of(page, size));
                        break;
                    case "seller":
                        passengerPage = passengerRepository.findBySellerEmailContainingIgnoreCase(
                            cleanSearch, PageRequest.of(page, size));
                        break;
                    default: // "all"
                        passengerPage = passengerRepository.findBySearchAll(
                            cleanSearch, PageRequest.of(page, size));
                        break;
                }
            } else if (soldStatus != null) {
                // Only status filter
                passengerPage = passengerRepository.findBySold(soldStatus, PageRequest.of(page, size));
            } else {
                // No filters
                passengerPage = passengerRepository.findAll(PageRequest.of(page, size));
            }
            
            // Calculate statistics based on current filters
            long totalTickets = passengerPage.getTotalElements();
            long soldTickets;
            long availableTickets;
            
            if (cleanSearch != null) {
                // If there's a search, calculate filtered statistics
                switch (searchType) {
                    case "pnr":
                        soldTickets = passengerRepository.countByPnrPnrNumberContainingIgnoreCaseAndSold(cleanSearch, true);
                        availableTickets = passengerRepository.countByPnrPnrNumberContainingIgnoreCaseAndSold(cleanSearch, false);
                        break;
                    case "passenger":
                        soldTickets = passengerRepository.countByNameContainingIgnoreCaseAndSold(cleanSearch, true);
                        availableTickets = passengerRepository.countByNameContainingIgnoreCaseAndSold(cleanSearch, false);
                        break;
                    case "seller":
                        soldTickets = passengerRepository.countBySellerEmailContainingIgnoreCaseAndSold(cleanSearch, true);
                        availableTickets = passengerRepository.countBySellerEmailContainingIgnoreCaseAndSold(cleanSearch, false);
                        break;
                    default: // "all"
                        soldTickets = passengerRepository.countBySearchAllAndSold(cleanSearch, true);
                        availableTickets = passengerRepository.countBySearchAllAndSold(cleanSearch, false);
                        break;
                }
            } else {
                // No search, use global statistics
                soldTickets = passengerRepository.countBySoldTrue();
                availableTickets = passengerRepository.countBySoldFalse();
            }
            
            // Add model attributes
            model.addAttribute("passengers", passengerPage.getContent());
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", passengerPage.getTotalPages());
            model.addAttribute("currentSize", size);
            model.addAttribute("totalTickets", totalTickets);
            model.addAttribute("soldTickets", soldTickets);
            model.addAttribute("availableTickets", availableTickets);
            model.addAttribute("currentSearch", search);
            model.addAttribute("currentSearchType", searchType);
            model.addAttribute("currentStatus", status);
            
            return "adminViewTickets";
        }
        
        @GetMapping("/users")
        public String viewAllUsers(
                @RequestParam(defaultValue = "0") int page,
                @RequestParam(defaultValue = "50") int size,
                @RequestParam(value = "search", required = false) String search,
                @RequestParam(value = "searchType", required = false, defaultValue = "email") String searchType,
                @RequestParam(value = "status", required = false) String status,
                Model model) {
            
            Page<User> userPage;
            
            // Clean up search parameter
            String cleanSearch = (search != null && !search.trim().isEmpty()) ? search.trim() : null;
            String cleanStatus = (status != null && !status.equals("all")) ? status : null;
            
            // Apply search and status filter
            if (cleanSearch != null && cleanStatus != null) {
                // Both search and status filter
                if ("all".equals(searchType)) {
                    userPage = userRepository.findBySearchAllAndStatus(cleanSearch, cleanStatus, PageRequest.of(page, size));
                } else {
                    userPage = userRepository.findByEmailContainingIgnoreCaseAndStatus(cleanSearch, cleanStatus, PageRequest.of(page, size));
                }
            } else if (cleanSearch != null) {
                // Only search filter
                if ("all".equals(searchType)) {
                    userPage = userRepository.findBySearchAll(cleanSearch, PageRequest.of(page, size));
                } else {
                    userPage = userRepository.findByEmailContainingIgnoreCase(cleanSearch, PageRequest.of(page, size));
                }
            } else if (cleanStatus != null) {
                // Only status filter
                userPage = userRepository.findByStatus(cleanStatus, PageRequest.of(page, size));
            } else {
                // No filters
                userPage = userRepository.findAll(PageRequest.of(page, size));
            }
            
            // Build user data with additional information
            List<Map<String, Object>> userData = new ArrayList<>();
            for (User user : userPage.getContent()) {
                Map<String, Object> data = new HashMap<>();
                data.put("user", user);
                
                // Get wallet balance
                Optional<Wallet> wallet = walletRepository.findByEmail(user.getEmail());
                data.put("walletBalance", wallet.map(Wallet::getBalance).orElse(0.0));
                
                // Get referral count
                int referralCount = userRepository.findAllByReferredBy(user.getReferralCode()).size();
                data.put("referralCount", referralCount);
                
                // Set status
                data.put("status", user.getStatus() != null ? user.getStatus() : "ACTIVE");
                
                userData.add(data);
            }
            
            // Add model attributes
            model.addAttribute("userData", userData);
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", userPage.getTotalPages());
            model.addAttribute("totalUsers", userPage.getTotalElements());
            model.addAttribute("currentSearch", search);
            model.addAttribute("currentSearchType", searchType);
            model.addAttribute("currentStatus", status);
            
            return "adminViewUsers";
        }

        // Helper method to build redirect URL with preserved parameters (for use in existing block/unblock methods)
        private String buildRedirectUrl(String baseUrl, String search, String searchType, String status, String page) {
            StringBuilder url = new StringBuilder("redirect:" + baseUrl);
            List<String> params = new ArrayList<>();
            
            if (search != null && !search.trim().isEmpty()) {
                params.add("search=" + search);
            }
            if (searchType != null && !searchType.equals("email")) {
                params.add("searchType=" + searchType);
            }
            if (status != null && !status.equals("all")) {
                params.add("status=" + status);
            }
            if (page != null && !page.equals("0")) {
                params.add("page=" + page);
            }
            
            if (!params.isEmpty()) {
                url.append("?").append(String.join("&", params));
            }
            
            return url.toString();
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
        public String showTransactions(
                Model model,
                @RequestParam(defaultValue = "0") int page,
                @RequestParam(defaultValue = "50") int size,
                @RequestParam(value = "search", required = false) String search,
                @RequestParam(value = "searchType", required = false, defaultValue = "all") String searchType) {
            
            Page<Passenger> pageResult;
            Double totalRevenue = 0.0;
            
            // Clean up search parameter
            String cleanSearch = (search != null && !search.trim().isEmpty()) ? search.trim() : null;
            
            Pageable pageable = PageRequest.of(page, size);
            
            try {
                // Apply search filter
                if (cleanSearch != null) {
                    switch (searchType) {
                        case "passenger":
                            pageResult = passengerRepository.findByNameContainingIgnoreCaseAndSold(
                                cleanSearch, true, pageable);
                            totalRevenue = passengerRepository.calculateRevenueByNameSearch(cleanSearch);
                            break;
                        case "buyer":
                            pageResult = passengerRepository.findByBuyerEmailContainingIgnoreCaseAndSold(
                                cleanSearch, true, pageable);
                            totalRevenue = passengerRepository.calculateRevenueByBuyerEmailSearch(cleanSearch);
                            break;
                        case "seller":
                            pageResult = passengerRepository.findBySellerEmailContainingIgnoreCaseAndSold(
                                cleanSearch, true, pageable);
                            totalRevenue = passengerRepository.calculateRevenueBySellerEmailSearch(cleanSearch);
                            break;
                        case "paymentId":
                            pageResult = passengerRepository.findByRazorpayPaymentIdContainingIgnoreCaseAndSold(
                                cleanSearch, true, pageable);
                            totalRevenue = passengerRepository.calculateRevenueByPaymentIdSearch(cleanSearch);
                            break;
                        default: // "all"
                            pageResult = passengerRepository.findBySoldTrueAndSearchAllTransactions(cleanSearch, pageable);
                            totalRevenue = passengerRepository.calculateRevenueBySearchAllTransactions(cleanSearch);
                            break;
                    }
                } else {
                    // No search filter, get all sold passengers
                    pageResult = passengerRepository.findBySoldTrue(pageable);
                    totalRevenue = passengerRepository.calculateTotalRevenue();
                }
                
                // Handle null revenue
                if (totalRevenue == null) {
                    totalRevenue = 0.0;
                }
                
            } catch (Exception e) {
                // Log error and provide fallback
                System.err.println("Error in transaction search: " + e.getMessage());
                pageResult = passengerRepository.findBySoldTrue(pageable);
                totalRevenue = 0.0;
            }
            
            // Add model attributes
            model.addAttribute("transactions", pageResult.getContent());
            model.addAttribute("totalRevenue", totalRevenue);
            model.addAttribute("totalPages", pageResult.getTotalPages());
            model.addAttribute("currentPage", page);
            model.addAttribute("currentSize", size);
            model.addAttribute("totalTransactions", pageResult.getTotalElements());
            model.addAttribute("currentSearch", search);
            model.addAttribute("currentSearchType", searchType);
            
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
            return "redirect:/admin/support/messages";
        }




}