package com.example.SwapTicket.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

@Service
public class AdminAuthService {
    
    @Autowired
    private PasswordService passwordService;
    
    @Value("${admin.email}")
    private String adminEmail;
    
    @Value("${admin.password}")
    private String adminPassword;
    
    private String hashedAdminPassword;
    
    @PostConstruct
    public void init() {
        // In a real application, you would store the hashed password in database
        // For now, we'll hash the configured password on startup
        if (adminPassword != null && !adminPassword.trim().isEmpty()) {
            // Check if it's already hashed (BCrypt hashes start with $2a$, $2b$, or $2y$)
            if (adminPassword.startsWith("$2a$") || adminPassword.startsWith("$2b$") || adminPassword.startsWith("$2y$")) {
                hashedAdminPassword = adminPassword;
            } else {
                hashedAdminPassword = passwordService.encodePassword(adminPassword);
                System.out.println("SECURITY WARNING: Admin password should be pre-hashed in production!");
                System.out.println("Replace admin.password in properties with: " + hashedAdminPassword);
            }
        }
    }
    
    /**
     * Authenticate admin user with email and password
     */
    public boolean authenticateAdmin(String email, String password) {
        if (email == null || password == null) {
            return false;
        }
        
        // Check email match
        if (!adminEmail.equals(email)) {
            return false;
        }
        
        // Check password match
        if (hashedAdminPassword == null) {
            return false;
        }
        
        return passwordService.matches(password, hashedAdminPassword);
    }
    
    /**
     * Get admin email
     */
    public String getAdminEmail() {
        return adminEmail;
    }
}