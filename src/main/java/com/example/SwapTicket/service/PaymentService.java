package com.example.SwapTicket.service;

import com.example.SwapTicket.model.Ticket;
import com.example.SwapTicket.model.Wallet;
import com.example.SwapTicket.repository.TicketRepository;
import com.example.SwapTicket.repository.WalletRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class PaymentService {

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private TicketRepository ticketRepository;

    public boolean transferToSeller(Ticket ticket) {
        Optional<Wallet> sellerWalletOpt = walletRepository.findByEmail(ticket.getSellerEmail());
        Optional<Wallet> adminWalletOpt = walletRepository.findByEmail("admin@swapticket.com");

        if (sellerWalletOpt.isPresent() && adminWalletOpt.isPresent()) {
            Wallet sellerWallet = sellerWalletOpt.get();
            Wallet adminWallet = adminWalletOpt.get();

            double ticketPrice = ticket.getPrice();

            if (adminWallet.getBalance() < ticketPrice) {
                return false;
            }

            adminWallet.setBalance(adminWallet.getBalance() - ticketPrice);
            walletRepository.save(adminWallet);

            sellerWallet.setBalance(sellerWallet.getBalance() + ticketPrice);
            walletRepository.save(sellerWallet);

            ticket.setSellerPaid(true);
            ticketRepository.save(ticket);

            return true;
        }

        return false;
    }
}