package com.example.SwapTicket.service;

import com.example.SwapTicket.model.Passenger;
import com.example.SwapTicket.model.Wallet;
import com.example.SwapTicket.repository.PassengerRepository;
import com.example.SwapTicket.repository.WalletRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class PaymentService {

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private PassengerRepository passengerRepository;

    public boolean paySeller(Passenger passenger) {
        Wallet sellerWallet = walletRepository.findByEmail(passenger.getSellerEmail())
                .orElseThrow(() -> new RuntimeException("Seller wallet not found"));
        Wallet adminWallet = walletRepository.findByEmail("admin@swapticket.com")
                .orElseThrow(() -> new RuntimeException("Admin wallet not found"));

        double ticketPrice = passenger.getPrice();

        if (adminWallet.getBalance() < ticketPrice) {
            return false;
        }

        adminWallet.setBalance(adminWallet.getBalance() - ticketPrice);
        walletRepository.save(adminWallet);

        sellerWallet.setBalance(sellerWallet.getBalance() + ticketPrice);
        walletRepository.save(sellerWallet);

        passenger.setSellerPaid(true);
        passengerRepository.save(passenger);

        return true;
    }
}