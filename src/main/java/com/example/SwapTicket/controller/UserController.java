package com.example.SwapTicket.controller;

import com.example.SwapTicket.model.User;
import com.example.SwapTicket.repository.WalletRepository;
import com.example.SwapTicket.service.UserService;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
public class UserController {
	

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private UserService userService;
    

    @GetMapping("/")
    public String defaultPage(HttpSession session) {
        // Check if the user is logged in
        if (session.getAttribute("loggedInUserEmail") != null) {
            return "redirect:/user/home";  // Redirect logged-in users to the dashboard
        }
        return "home";  // Otherwise, show the home page (landing page with login/register buttons)
    }


    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @PostMapping("/login")
    public String loginUser(@RequestParam String username,
                            @RequestParam String password,
                            Model model,
                            HttpSession session) {
    	if (username.equals("admin@swapticket.com") && password.equals("admin")) { // use your configured password
            session.setAttribute("admin", true);
            return "redirect:/admin/dashboard";
        }

        User user = userService.findByEmail(username);
        
        if (user == null) {
            model.addAttribute("loginError", "Enter correct email");
            return "login";
        } else if (!user.getPassword().equals(password)) {
            model.addAttribute("loginError", "Enter correct password");
            return "login";
        } else {
            session.setAttribute("loggedInUserEmail", user.getEmail());
            session.setAttribute("loggedInUserName", user.getName());
            model.addAttribute("message", "Welcome back, " + user.getName() + "!");
            return "redirect:/user/home";
        }
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("user", new User());
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(@Valid @ModelAttribute("user") User user,
                               BindingResult result,
                               Model model) {
        if (result.hasErrors()) {
            return "register";
        }
        if (userService.existsByEmail(user.getEmail())) {
            model.addAttribute("emailError", "Email is already registered");
            return "register";
        }
        if (!user.getPassword().equals(user.getConfirmPassword())) {
            model.addAttribute("passwordError", "Passwords do not match");
            return "register";
        }

        userService.saveUser(user);
        return "redirect:/login";
    }

    @GetMapping("/user/sell")
    public String sellTicket() {
        return "sellTicket"; 
    }

    @GetMapping("/user/home")
    public String userHome(Model model, HttpSession session) {
        String userEmail = (String) session.getAttribute("loggedInUserEmail");
        model.addAttribute("userName", userEmail);

        walletRepository.findByEmail(userEmail).ifPresent(wallet -> {
            model.addAttribute("walletBalance", wallet.getBalance());
        });

        return "userHome";
    }


    @GetMapping("/forgot-password")
    public String forgotPasswordPage() {
        return "forgotPassword";
    }

    @PostMapping("/forgot-password")
    public String resetPassword(@RequestParam("email") String email,
                                @RequestParam("newPassword") String newPassword,
                                @RequestParam("confirmPassword") String confirmPassword,
                                Model model) {
        if (!newPassword.equals(confirmPassword)) {
            model.addAttribute("error", "Passwords do not match.");
            return "forgotPassword";
        }

        User user = userService.findByEmail(email);
        if (user == null) {
            model.addAttribute("error", "No user found with this email.");
            return "forgotPassword";
        }

        user.setPassword(newPassword);
        userService.saveUser(user);
        model.addAttribute("success", "Password reset successfully!");
        return "login";
    }
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate(); // Clear session
        return "home";
    }


}
