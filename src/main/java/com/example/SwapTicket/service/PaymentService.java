package com.example.SwapTicket.service;

import com.example.SwapTicket.model.Passenger;
import com.example.SwapTicket.model.Wallet;
import com.example.SwapTicket.repository.PassengerRepository;
import com.example.SwapTicket.repository.WalletRepository;

import jakarta.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class PaymentService {

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private PassengerRepository passengerRepository;
    
    @Value("${admin.email}")
    String ADMIN_EMAIL;
    
    @Transactional
    public synchronized boolean paySeller(Passenger passenger) {
        if (passenger == null) {
            throw new IllegalArgumentException("Passenger cannot be null");
        }
        
        if (passenger.isSellerPaid()) {
            throw new IllegalStateException("Seller has already been paid for this passenger");
        }
        
        Wallet sellerWallet = walletRepository.findByEmail(passenger.getSellerEmail())
                .orElseThrow(() -> new RuntimeException("Seller wallet not found for email: " + passenger.getSellerEmail()));
        Wallet adminWallet = walletRepository.findByEmail(ADMIN_EMAIL)
                .orElseThrow(() -> new RuntimeException("Admin wallet not found"));

        double ticketPrice = passenger.getPrice();
        
        if (ticketPrice <= 0) {
            throw new IllegalArgumentException("Ticket price must be positive");
        }

        if (adminWallet.getBalance() < ticketPrice) {
            throw new IllegalStateException("Insufficient admin wallet balance. Required: " + ticketPrice + ", Available: " + adminWallet.getBalance());
        }

        // Perform atomic wallet operations
        adminWallet.deductBalance(ticketPrice);
        sellerWallet.addBalance(ticketPrice);
        
        // Save wallets
        walletRepository.save(adminWallet);
        walletRepository.save(sellerWallet);

        // Mark passenger as paid
        passenger.setSellerPaid(true);
        passengerRepository.save(passenger);

        return true;
    }
    
    @Transactional
    public synchronized boolean buyPassenger(Long passengerId, String buyerEmail) {
        if (passengerId == null) {
            throw new IllegalArgumentException("Passenger ID cannot be null");
        }
        
        if (buyerEmail == null || buyerEmail.trim().isEmpty()) {
            throw new IllegalArgumentException("Buyer email cannot be null or empty");
        }
        
        Passenger passenger = passengerRepository.findById(passengerId)
            .orElseThrow(() -> new RuntimeException("Passenger not found with ID: " + passengerId));

        if (passenger.isSold()) {
            throw new IllegalStateException("Passenger already sold");
        }

        // Set details atomically
        passenger.setBuyerEmail(buyerEmail.trim());
        passenger.setSold(true);
        passengerRepository.save(passenger);
        
        return true;
    }
    
    @Transactional
    public synchronized boolean processPayment(String buyerEmail, double amount, String adminEmail) {
        if (buyerEmail == null || buyerEmail.trim().isEmpty()) {
            throw new IllegalArgumentException("Buyer email cannot be null or empty");
        }
        
        if (amount <= 0) {
            throw new IllegalArgumentException("Payment amount must be positive");
        }
        
        if (adminEmail == null || adminEmail.trim().isEmpty()) {
            throw new IllegalArgumentException("Admin email cannot be null or empty");
        }
        
        // Get or create admin wallet
        Wallet adminWallet = walletRepository.findByEmail(adminEmail)
                .orElseGet(() -> {
                    Wallet newWallet = new Wallet();
                    newWallet.setEmail(adminEmail);
                    newWallet.setBalance(0.0);
                    return walletRepository.save(newWallet);
                });
        
        // Add payment to admin wallet
        adminWallet.addBalance(amount);
        walletRepository.save(adminWallet);
        
        return true;
    }
}