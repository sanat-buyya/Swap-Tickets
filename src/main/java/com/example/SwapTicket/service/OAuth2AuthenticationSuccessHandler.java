package com.example.SwapTicket.service;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private static final Logger logger = LoggerFactory.getLogger(OAuth2AuthenticationSuccessHandler.class);

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                      Authentication authentication) throws IOException, ServletException {
        
        CustomOAuth2User oAuth2User = (CustomOAuth2User) authentication.getPrincipal();
        
        // Set session attributes for OAuth2 user
        HttpSession session = request.getSession();
        session.setAttribute("loggedInUserEmail", oAuth2User.getEmail());
        session.setAttribute("loggedInUserName", oAuth2User.getName());
        session.setAttribute("userProfileImage", oAuth2User.getProfileImageUrl());
        session.setAttribute("loginType", "oauth2");
        
        logger.info("OAuth2 user {} logged in successfully", oAuth2User.getEmail());
        
        // Check if user needs to complete profile
        if (needsProfileCompletion(oAuth2User)) {
            response.sendRedirect("/user/complete-profile");
        } else {
            response.sendRedirect("/user/home");
        }
    }

    private boolean needsProfileCompletion(CustomOAuth2User oAuth2User) {
        // Check if user needs to provide additional information
        String mobile = oAuth2User.getUser().getMobile();
        return mobile == null || mobile.trim().isEmpty();
    }
}