package com.example.SwapTicket.service;

import com.example.SwapTicket.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.UUID;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    @Autowired
    private UserService userService;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(userRequest);
        
        // Get user information from Google
        String email = oauth2User.getAttribute("email");
        String name = oauth2User.getAttribute("name");
        String googleId = oauth2User.getAttribute("sub");
        
        // Check if user already exists
        User existingUser = userService.findByEmail(email);
        
        if (existingUser == null) {
            // Create new user for Google OAuth2
            User newUser = new User();
            newUser.setEmail(email);
            newUser.setName(name != null ? name : "Google User");
            newUser.setProvider("GOOGLE");
            newUser.setGoogleId(googleId);
            
            // Set default values for required fields to avoid validation errors
            newUser.setMobile("9999999999"); // Valid default mobile that passes validation
            newUser.setDob(LocalDate.of(1990, 1, 1)); // Default DOB, user can update later
            newUser.setPassword("GoogleOAuth2User@123"); // Set a default password that meets validation rules
            newUser.setReferralCode(generateReferralCode());
            newUser.setStatus("ACTIVE");
            
            try {
                userService.saveUser(newUser);
                System.out.println("Successfully created Google user: " + email);
            } catch (Exception e) {
                System.err.println("Error creating Google user: " + e.getMessage());
                e.printStackTrace();
                throw new OAuth2AuthenticationException("Failed to create user account");
            }
        } else if ("LOCAL".equals(existingUser.getProvider())) {
            // Link existing local account with Google
            existingUser.setProvider("GOOGLE");
            existingUser.setGoogleId(googleId);
            try {
                userService.saveUser(existingUser);
                System.out.println("Successfully linked existing user with Google: " + email);
            } catch (Exception e) {
                System.err.println("Error linking user with Google: " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        return oauth2User;
    }
    
    private String generateReferralCode() {
        return "REF" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}