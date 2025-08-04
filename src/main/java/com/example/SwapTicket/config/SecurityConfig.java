package com.example.SwapTicket.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.web.http.CookieHttpSessionIdResolver;
import org.springframework.session.web.http.HttpSessionIdResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

@Configuration
public class SecurityConfig implements WebMvcConfigurer {

    @Bean
    public HttpSessionIdResolver httpSessionIdResolver() {
        CookieHttpSessionIdResolver resolver = CookieHttpSessionIdResolver.cookieName("SESSIONID");
        resolver.setCookieMaxAge(1800); // 30 minutes
        return resolver;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SessionValidationInterceptor())
                .addPathPatterns("/user/**", "/pnr/**", "/admin/**")
                .excludePathPatterns("/", "/login", "/register", "/otp", "/forgot-password", "/api/**", 
                                   "/privacy", "/terms", "/about", "/how-it-works", "/faq", "/refund", "/maskedAadhar");
    }

    public static class SessionValidationInterceptor implements HandlerInterceptor {
        
        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
            String requestURI = request.getRequestURI();
            
            // Check admin routes
            if (requestURI.startsWith("/admin/")) {
                Boolean isAdmin = (Boolean) request.getSession().getAttribute("admin");
                if (isAdmin == null || !isAdmin) {
                    response.sendRedirect("/login");
                    return false;
                }
            }
            
            // Check user routes
            if (requestURI.startsWith("/user/") || requestURI.startsWith("/pnr/")) {
                String userEmail = (String) request.getSession().getAttribute("loggedInUserEmail");
                if (userEmail == null || userEmail.trim().isEmpty()) {
                    response.sendRedirect("/login");
                    return false;
                }
            }
            
            // Add security headers
            response.setHeader("X-Content-Type-Options", "nosniff");
            response.setHeader("X-Frame-Options", "DENY");
            response.setHeader("X-XSS-Protection", "1; mode=block");
            
            return true;
        }
    }
}