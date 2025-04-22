package com.example.SwapTicket.controller;

import com.example.SwapTicket.model.User;
import com.example.SwapTicket.model.Wallet;
import com.example.SwapTicket.repository.WalletRepository;
import com.example.SwapTicket.service.UserService;
import com.example.SwapTicket.helper.EmailSender;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

import java.util.Optional; // Replace with your actual package
import java.util.Random;

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
    
    @Autowired
	EmailSender emailSender;
    
    @GetMapping("/")
    public String defaultPage(HttpSession session) {
        // Check if the user is logged in
        if (session.getAttribute("loggedInUserEmail") != null) {
            return "redirect:/user/home";  // Redirect logged-in users to the dashboard
        }
        return "home";  
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
    	if (username.equals("admin@swapticket.com") && password.equals("admin")) {
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
                               Model model,HttpSession session) {
    	if (userService.existsByEmail(user.getEmail())) {
            model.addAttribute("email", "Email is already registered");
            result.rejectValue("email", "error.email", "* Email Already Exists");
            return "register";
        } 
        if (!user.getPassword().equals(user.getConfirmPassword())) {
            model.addAttribute("passwordError", "Passwords do not match");
            return "register";
        }
        if (result.hasErrors()) {
            return "register";
        }
        int otp = new Random().nextInt(100000, 1000000);
        emailSender.sendEmail(user, otp);

        session.setAttribute("otp", otp);
        session.setAttribute("userDto", user); // Store temporarily before OTP verify
        session.setAttribute("pass", "Otp Sent Success");

        return "redirect:/otp";
    }
    @GetMapping("/otp")
    public String showOtpPage() {
        return "otp";
    }
    @PostMapping("/otp")
    public String verifyOtp(@RequestParam("otp") int userOtp, HttpSession session, Model model) {
        Integer sessionOtp = (Integer) session.getAttribute("otp");
        User user = (User) session.getAttribute("userDto");

        if (sessionOtp == null || user == null) {
            model.addAttribute("error", "Session expired. Please register again.");
            return "register";
        }

        if (userOtp == sessionOtp) {
            userService.saveUser(user); // Save only after OTP is verified

            // Clean up session
            session.removeAttribute("otp");
            session.removeAttribute("userDto");
            
            userService.saveUser(user);
            return "redirect:/login";
        } else {
            model.addAttribute("otpError", "Invalid OTP. Please try again.");
            return "otp";
        }
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
    @GetMapping("/wallet/withdraw")
    public String showWithdrawForm(HttpSession session, Model model) {
    	String userEmail = (String) session.getAttribute("loggedInUserEmail");
        if (userEmail == null) {
            return "redirect:/login";
        }

        Optional<Wallet> walletOpt = walletRepository.findByEmail(userEmail);
        if (walletOpt.isPresent()) {
            model.addAttribute("walletBalance", walletOpt.get().getBalance());
        } else {
            model.addAttribute("walletBalance", 0.0);
        }

        return "withdrawForm";
    }



}
