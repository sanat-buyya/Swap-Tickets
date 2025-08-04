package com.example.SwapTicket.controller;

import com.example.SwapTicket.model.User;
import com.example.SwapTicket.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Map;

@Controller
@RequestMapping("/oauth2")
public class OAuth2LoginController {

    @Autowired
    private UserService userService;

    @GetMapping("/login/success")
    public String loginSuccess(@AuthenticationPrincipal OAuth2User oauth2User, HttpSession session) {
        if (oauth2User == null) {
            return "redirect:/login?error=oauth_failed";
        }

        try {
            // Extract user information from OAuth2User
            Map<String, Object> attributes = oauth2User.getAttributes();
            String email = (String) attributes.get("email");
            String name = (String) attributes.get("name");
            String googleId = (String) attributes.get("sub");

            if (email == null || name == null) {
                return "redirect:/login?error=missing_info";
            }

            // Check if user already exists
            User existingUser = userService.findByEmail(email);
            
            if (existingUser == null) {
                // Create new user for Google OAuth
                User newUser = new User();
                newUser.setEmail(email);
                newUser.setName(name);
                newUser.setGoogleId(googleId);
                newUser.setEmailVerified(true); // Google emails are pre-verified
                
                // Save the new user
                userService.saveOAuth2User(newUser);
                existingUser = newUser;
            } else {
                // Update existing user with Google ID if not set
                if (existingUser.getGoogleId() == null) {
                    existingUser.setGoogleId(googleId);
                    existingUser.setEmailVerified(true);
                    userService.updateUser(existingUser);
                }
            }

            // Set session attributes
            session.setAttribute("loggedInUserEmail", existingUser.getEmail());
            session.setAttribute("loggedInUserName", existingUser.getName());
            session.setAttribute("oauth2User", true);

            return "redirect:/user/home";
            
        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/login?error=oauth_processing_failed";
        }
    }
}