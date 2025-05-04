package com.example.SwapTicket.controller;

import com.example.SwapTicket.model.Ticket;
import com.example.SwapTicket.model.Wallet;
import com.example.SwapTicket.repository.TicketRepository;
import com.example.SwapTicket.repository.WalletRepository;
import com.example.SwapTicket.service.PaymentService;

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
	private PaymentService paymentService;


    private final String ADMIN_EMAIL = "admin@swapticket.com"; 
    
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

        // Filter to show only sold but unpaid tickets
        List<Ticket> unpaidTickets = allTickets.stream()
            .filter(ticket -> ticket.isSold() && !ticket.isSellerPaid())
            .toList();

        Wallet adminWallet = walletRepository.findByEmail(ADMIN_EMAIL).orElse(new Wallet());

        model.addAttribute("tickets", unpaidTickets);
        model.addAttribute("adminWallet", adminWallet.getBalance());

        return "adminDashboard";
    }


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

        boolean success = paymentService.transferToSeller(ticket);

        if (!success) {
            model.addAttribute("error", "Payment failed: Insufficient balance or wallet not found");
            return "redirect:/admin/dashboard";
        }

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
    @GetMapping("/withdraw/form/{walletId}")
    public String showWithdrawForm(@PathVariable Long walletId, Model model, HttpSession session) {
        if (session.getAttribute("admin") == null) {
            return "redirect:/login";
        }

        Optional<Wallet> walletOpt = walletRepository.findById(walletId);
        if (walletOpt.isEmpty()) {
            model.addAttribute("error", "Wallet not found");
            return "redirect:/admin/wallets";
        }

        model.addAttribute("wallet", walletOpt.get());
        return "withdrawForm";
    }

}