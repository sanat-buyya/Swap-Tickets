package com.example.SwapTicket.controller;

import com.example.SwapTicket.model.Ticket;
import com.example.SwapTicket.model.Wallet;
import com.example.SwapTicket.repository.TicketRepository;
import com.example.SwapTicket.repository.WalletRepository;
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

    private final String ADMIN_EMAIL = "admin@swapticket.com"; // must match application.properties

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private WalletRepository walletRepository;

    // Admin Dashboard
    @GetMapping("/dashboard")
    public String adminDashboard(Model model, HttpSession session) {
        if (session.getAttribute("admin") == null) {
            return "redirect:/login";
        }

        List<Ticket> allTickets = ticketRepository.findAll();
        Wallet adminWallet = walletRepository.findByEmail(ADMIN_EMAIL).orElse(new Wallet());

        model.addAttribute("tickets", allTickets);
        model.addAttribute("adminWallet", adminWallet.getBalance());

        return "adminDashboard"; // Make sure adminDashboard.html exists
    }

    // Pay seller
    @PostMapping("/pay-seller/{ticketId}")
    public String paySeller(@PathVariable Long ticketId, Model model, HttpSession session) {
        if (session.getAttribute("admin") == null) {
            return "redirect:/login";
        }

        Optional<Ticket> optionalTicket = ticketRepository.findById(ticketId);

        if (optionalTicket.isEmpty()) {
            model.addAttribute("error", "Ticket not found");
            return "redirect:/admin/dashboard";
        }

        Ticket ticket = optionalTicket.get();
        if (!ticket.isSold()) {
            model.addAttribute("error", "Ticket is not sold yet");
            return "redirect:/admin/dashboard";
        }

        Optional<Wallet> adminWalletOpt = walletRepository.findByEmail(ADMIN_EMAIL);
        Optional<Wallet> sellerWalletOpt = walletRepository.findByEmail(ticket.getSellerEmail());

        if (adminWalletOpt.isEmpty() || sellerWalletOpt.isEmpty()) {
            model.addAttribute("error", "Wallet not found");
            return "redirect:/admin/dashboard";
        }

        Wallet adminWallet = adminWalletOpt.get();
        Wallet sellerWallet = sellerWalletOpt.get();

        double price = ticket.getPrice();
        if (adminWallet.getBalance() < price) {
            model.addAttribute("error", "Insufficient admin balance");
            return "redirect:/admin/dashboard";
        }

        // Transfer money
        adminWallet.setBalance(adminWallet.getBalance() - price);
        sellerWallet.setBalance(sellerWallet.getBalance() + price);

        walletRepository.save(adminWallet);
        walletRepository.save(sellerWallet);

        model.addAttribute("success", "Seller paid successfully!");
        return "redirect:/admin/dashboard";
    }
    @GetMapping("/wallets")
    public String viewWallets(Model model, HttpSession session) {
        Boolean isAdmin = (Boolean) session.getAttribute("admin");
        if (isAdmin == null || !isAdmin) {
            return "redirect:/login";
        }

        List<Wallet> allWallets = walletRepository.findAll();
        model.addAttribute("wallets", allWallets);
        return "adminWallet";
    }

}
