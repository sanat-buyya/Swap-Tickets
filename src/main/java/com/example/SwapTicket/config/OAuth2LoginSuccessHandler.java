package com.example.SwapTicket.config;

import com.example.SwapTicket.model.User;
import com.example.SwapTicket.service.UserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    @Autowired
    private UserService userService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        
        try {
            OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
            String email = oauth2User.getAttribute("email");
            String name = oauth2User.getAttribute("name");
            
            System.out.println("OAuth2 Success Handler - Email: " + email + ", Name: " + name);
            
            // Find the user in our database
            User user = userService.findByEmail(email);
            
            if (user != null) {
                // Set session attributes
                HttpSession session = request.getSession();
                session.setAttribute("loggedInUserEmail", user.getEmail());
                session.setAttribute("loggedInUserName", user.getName());
                session.setAttribute("oauthUser", true);
                
                System.out.println("OAuth2 login successful for user: " + user.getEmail());
                
                // Redirect to user home page
                response.sendRedirect("/user/home");
            } else {
                // This shouldn't happen as the user should be created in CustomOAuth2UserService
                System.err.println("User not found in database after OAuth2 authentication: " + email);
                response.sendRedirect("/login?error=oauth");
            }
        } catch (Exception e) {
            System.err.println("Error in OAuth2 success handler: " + e.getMessage());
            e.printStackTrace();
            response.sendRedirect("/login?error=oauth");
        }
    }
}