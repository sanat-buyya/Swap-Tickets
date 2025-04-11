package com.example.SwapTicket.controller;

import com.example.SwapTicket.model.Ticket;
import com.example.SwapTicket.model.User;
import com.example.SwapTicket.service.UserService;
import jakarta.validation.Valid;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
public class UserController {

    @Autowired
    private UserService userService;
    
    
    @GetMapping("/")
    public String defaultPage() {
        return "redirect:/login";
    }

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @PostMapping("/login")
    public String loginUser(@RequestParam String username,
                            @RequestParam String password,
                            Model model) {
        User user = userService.findByEmail(username);
        if (user == null) {
            model.addAttribute("loginError", "Enter correct email");
            return "login";
        } else if (!user.getPassword().equals(password)) {
            model.addAttribute("loginError", "Enter correct password");
            return "login";
        } else {
            model.addAttribute("message", "Welcome back, " + user.getName() + "!");
            return "userHome";
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
        return "sellTicket"; // create sellTicket.html
    }
    
    
 
    
    
    
}
