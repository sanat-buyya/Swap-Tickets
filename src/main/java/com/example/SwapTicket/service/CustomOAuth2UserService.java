package com.example.SwapTicket.service;

import com.example.SwapTicket.model.User;
import com.example.SwapTicket.model.Wallet;
import com.example.SwapTicket.repository.UserRepository;
import com.example.SwapTicket.repository.WalletRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private static final Logger logger = LoggerFactory.getLogger(CustomOAuth2UserService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        
        try {
            return processOAuth2User(userRequest, oAuth2User);
        } catch (Exception ex) {
            logger.error("Error processing OAuth2 user: {}", ex.getMessage(), ex);
            throw new OAuth2AuthenticationException("Error processing OAuth2 user");
        }
    }

    private OAuth2User processOAuth2User(OAuth2UserRequest userRequest, OAuth2User oAuth2User) {
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        String providerId = oAuth2User.getAttribute("sub"); // Google user ID
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        String profileImageUrl = oAuth2User.getAttribute("picture");

        if (email == null || email.trim().isEmpty()) {
            throw new OAuth2AuthenticationException("Email not found from OAuth2 provider");
        }

        User user = userRepository.findByEmail(email);
        
        if (user != null) {
            // Update existing user with OAuth2 info if needed
            if (user.getProvider() == null || !user.getProvider().equals(registrationId)) {
                user.setProvider(registrationId);
                user.setProviderId(providerId);
                user.setProfileImageUrl(profileImageUrl);
                userRepository.save(user);
                logger.info("Updated existing user {} with OAuth2 info", email);
            }
        } else {
            // Create new user from OAuth2 info
            user = createOAuth2User(registrationId, providerId, email, name, profileImageUrl);
            logger.info("Created new OAuth2 user: {}", email);
        }

        return new CustomOAuth2User(oAuth2User, user);
    }

    private User createOAuth2User(String provider, String providerId, String email, String name, String profileImageUrl) {
        User user = new User();
        user.setEmail(email);
        user.setName(name != null ? name : extractNameFromEmail(email));
        user.setProvider(provider);
        user.setProviderId(providerId);
        user.setProfileImageUrl(profileImageUrl);
        user.setStatus("ACTIVE");
        
        // Generate unique referral code
        String referralCode = email.substring(0, Math.min(4, email.indexOf('@'))).toUpperCase()
                + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        user.setReferralCode(referralCode);
        
        // Set default values for OAuth2 users
        user.setMobile(""); // Will need to be filled later
        user.setDob(LocalDate.of(1990, 1, 1)); // Default DOB, can be updated later
        
        // Save user
        user = userRepository.save(user);
        
        // Create wallet for new user
        createWalletForUser(user.getEmail());
        
        return user;
    }

    private void createWalletForUser(String email) {
        try {
            Wallet wallet = new Wallet();
            wallet.setEmail(email);
            wallet.setBalance(0.0);
            walletRepository.save(wallet);
            logger.info("Created wallet for OAuth2 user: {}", email);
        } catch (Exception e) {
            logger.error("Failed to create wallet for OAuth2 user {}: {}", email, e.getMessage(), e);
        }
    }

    private String extractNameFromEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "User";
        }
        String localPart = email.substring(0, email.indexOf("@"));
        // Capitalize first letter and replace dots/underscores with spaces
        return localPart.substring(0, 1).toUpperCase() + 
               localPart.substring(1).replace(".", " ").replace("_", " ");
    }
}