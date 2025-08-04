package com.example.SwapTicket.service;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class PasswordService {
    
    private final BCryptPasswordEncoder passwordEncoder;
    
    public PasswordService() {
        this.passwordEncoder = new BCryptPasswordEncoder(12); // Strong cost factor
    }
    
    /**
     * Encode a raw password using BCrypt
     */
    public String encodePassword(String rawPassword) {
        if (rawPassword == null || rawPassword.trim().isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }
        return passwordEncoder.encode(rawPassword);
    }
    
    /**
     * Verify a raw password against an encoded password
     */
    public boolean matches(String rawPassword, String encodedPassword) {
        if (rawPassword == null || encodedPassword == null) {
            return false;
        }
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }
    
    /**
     * Check if a password needs to be re-encoded (for security upgrades)
     */
    public boolean needsUpgrade(String encodedPassword) {
        return passwordEncoder.upgradeEncoding(encodedPassword);
    }
}