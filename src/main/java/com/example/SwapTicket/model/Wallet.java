package com.example.SwapTicket.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

@Entity
@Table(name = "wallets")
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Column(unique = true, nullable = false)
    private String email; // user/seller/admin email
    
    @PositiveOrZero(message = "Balance cannot be negative")
    @Column(nullable = false)
    private double balance = 0.0;

    // Constructors
    public Wallet() {}
    
    public Wallet(String email, double balance) {
        this.email = email;
        this.balance = Math.max(0.0, balance); // Ensure non-negative
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        if (balance < 0) {
            throw new IllegalArgumentException("Balance cannot be negative. Attempted to set: " + balance);
        }
        this.balance = balance;
    }

    public synchronized void addBalance(double amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Amount to add must be positive. Provided: " + amount);
        }
        this.balance += amount;
    }

    public synchronized void deductBalance(double amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Amount to deduct must be positive. Provided: " + amount);
        }
        if (this.balance < amount) {
            throw new IllegalStateException("Insufficient balance. Current: " + this.balance + ", Required: " + amount);
        }
        this.balance -= amount;
    }
    
    public boolean hasSufficientBalance(double amount) {
        return this.balance >= amount;
    }
    
    @Override
    public String toString() {
        return "Wallet{" +
                "id=" + id +
                ", email='" + email + '\'' +
                ", balance=" + balance +
                '}';
    }
}