package com.example.SwapTicket.controller;

import com.example.SwapTicket.model.AdminConfig;
import com.example.SwapTicket.model.Passenger;
import com.example.SwapTicket.model.User;
import com.example.SwapTicket.model.Wallet;
import com.example.SwapTicket.repository.AdminConfigRepository;
import com.example.SwapTicket.repository.PassengerRepository;
import com.example.SwapTicket.repository.UserRepository;
import com.example.SwapTicket.repository.WalletRepository;
import com.example.SwapTicket.service.PaymentService;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;


import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

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
        private UserRepository userRepository;
        
        @Autowired
        private AdminConfigRepository adminConfigRepository;

        private static final String ADMIN_EMAIL = "admin@swapticket.com";

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

            redirectAttributes.addFlashAttribute("success", "Seller paid successfully!");
            return "redirect:/admin/dashboard";
        }

        @GetMapping("/fee")
        public String viewFeeSetting(Model model) {
            AdminConfig config = adminConfigRepository.findById(1L)
                .orElseGet(() -> {
                    AdminConfig newConfig = new AdminConfig();
                    newConfig.setBookingFee(20); // default
                    return adminConfigRepository.save(newConfig);
                });

            model.addAttribute("bookingFee", config.getBookingFee());
            return "adminFee";
        }

        @PostMapping("/fee")
        public String updateFee(@RequestParam("fee") double fee) {
            AdminConfig config = adminConfigRepository.findById(1L)
                .orElseThrow(() -> new RuntimeException("Admin config not found"));
            config.setBookingFee(fee);
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
                
                userData.add(data);
            }

            model.addAttribute("userData", userData);
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", userPage.getTotalPages());
            model.addAttribute("totalUsers", userPage.getTotalElements());

            return "adminViewUsers";
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






}