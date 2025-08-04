package com.example.SwapTicket.service;

import com.example.SwapTicket.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
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
            newUser.setName(name);
            newUser.setProvider("GOOGLE");
            newUser.setGoogleId(googleId);
            
            // Set default values for required fields
            newUser.setMobile("0000000000"); // Default mobile, user can update later
            newUser.setDob(LocalDate.of(1990, 1, 1)); // Default DOB, user can update later
            newUser.setPassword(""); // No password for OAuth2 users
            newUser.setReferralCode(generateReferralCode());
            newUser.setStatus("ACTIVE");
            
            userService.saveUser(newUser);
        } else if (existingUser.getProvider().equals("LOCAL")) {
            // Link existing local account with Google
            existingUser.setProvider("GOOGLE");
            existingUser.setGoogleId(googleId);
            userService.saveUser(existingUser);
        }
        
        return oauth2User;
    }
    
    private String generateReferralCode() {
        return "REF" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}