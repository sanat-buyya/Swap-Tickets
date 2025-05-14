package com.example.SwapTicket.controller;

import com.example.SwapTicket.model.Passenger;
import com.example.SwapTicket.model.Wallet;
import com.example.SwapTicket.repository.PassengerRepository;
import com.example.SwapTicket.repository.WalletRepository;
import com.example.SwapTicket.service.PaymentService;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;


import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
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
    


//    @GetMapping("/wallets")
//    public String viewWallets(Model model, HttpSession session) {
//        Boolean isAdmin = (Boolean) session.getAttribute("admin");
//        if (isAdmin == null || !isAdmin) {
//            return "redirect:/login";
//        }
//
//        List<Wallet> allWallets = walletRepository.findAll();
//        model.addAttribute("wallets", allWallets);
//        return "adminWallet";
//    }
//    @GetMapping("/withdraw/form/{walletId}")
//    public String showWithdrawForm(@PathVariable Long walletId, Model model, HttpSession session) {
//        if (session.getAttribute("admin") == null) {
//            return "redirect:/login";
//        }
//
//        Optional<Wallet> walletOpt = walletRepository.findById(walletId);
//        if (walletOpt.isEmpty()) {
//            model.addAttribute("error", "Wallet not found");
//            return "redirect:/admin/wallets";
//        }
//
//        model.addAttribute("wallet", walletOpt.get());
//        return "withdrawForm";
//    }

}