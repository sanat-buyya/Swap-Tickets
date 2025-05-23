package com.example.SwapTicket.controller;

import com.example.SwapTicket.model.PNR;
import com.example.SwapTicket.model.Passenger;
import com.example.SwapTicket.model.SupportMessage;
import com.example.SwapTicket.model.TransactionHistory;
import com.example.SwapTicket.model.User;
import com.example.SwapTicket.model.Wallet;
import com.example.SwapTicket.repository.PNRRepository;
import com.example.SwapTicket.repository.PassengerRepository;
import com.example.SwapTicket.repository.SupportMessageRepository;
import com.example.SwapTicket.repository.TransactionHistoryRepository;
import com.example.SwapTicket.repository.WalletRepository;
import com.example.SwapTicket.service.UserService;
import com.example.SwapTicket.helper.AES;
import com.example.SwapTicket.helper.EmailSender;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional; // Replace with your actual package
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
public class UserController {
	
	@Value("${admin.email}")
	String adminEmail;

	@Value("${admin.password}")
	String adminPassword;

    @Autowired
    private WalletRepository walletRepository;
    
    @Autowired
    private PNRRepository pnrRepository;

    @Autowired
    private PassengerRepository passengerRepository;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private TransactionHistoryRepository transactionHistoryRepository;
    
    @Autowired
    private SupportMessageRepository supportRepo;
    
    @Autowired
	EmailSender emailSender;
    
    @GetMapping("/")
    public String defaultPage(HttpSession session) { 
        if (session.getAttribute("loggedInUserEmail") != null) {
            return "redirect:/user/home";
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
        if (username.equals(adminEmail) && password.equals(adminPassword)) {
            session.setAttribute("admin", true);
            return "redirect:/admin/dashboard";
        }

        User user = userService.findByEmail(username);
        if (user == null) {
            model.addAttribute("loginError", "Enter correct email");
            return "login";
        }

        try {
            String decryptedPassword = AES.decrypt(user.getPassword());
            if (decryptedPassword == null || !decryptedPassword.equals(password)) {
                model.addAttribute("loginError", "Enter correct password");
                return "login";
            }
        } catch (Exception e) {
            model.addAttribute("loginError", "Error verifying password");
            return "login";
        }

        session.setAttribute("loggedInUserEmail", user.getEmail());
        session.setAttribute("loggedInUserName", user.getName());
        model.addAttribute("message", "Welcome back, " + user.getName() + "!");
        return "redirect:/user/home";
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
            String encryptedPassword = AES.encrypt(user.getPassword());
            if (encryptedPassword == null) {
                model.addAttribute("error", "Encryption error. Please try registering again.");
                return "register";
            }
            user.setPassword(encryptedPassword);
            user.setConfirmPassword(encryptedPassword); // Optional, to keep structure consistent

            userService.saveUser(user);

            // Create wallet
            Wallet wallet = new Wallet();
            wallet.setEmail(user.getEmail());
            wallet.setBalance(0.0);
            walletRepository.save(wallet);

            // Clear session attributes
            session.removeAttribute("otp");
            session.removeAttribute("userDto");

            return "redirect:/login";
        } else {
            model.addAttribute("otpError", "Invalid OTP. Please try again.");
            return "otp";
        }
    }


    @GetMapping("/user/sell")
    public String sellTicket() {
        return "sellTicket1"; 
    }

    @GetMapping("/user/home")
    public String userHome(Model model, HttpSession session) {
        String userEmail = (String) session.getAttribute("loggedInUserEmail");
        if (userEmail == null) {
            return "redirect:/login";
        }
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
        
        String encryptedPassword = AES.encrypt(newPassword);
        user.setPassword(encryptedPassword);
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
    
    @GetMapping("/my-listed-tickets")
    public String getMyListedTickets(HttpSession session, Model model) {
        String sellerEmail = (String) session.getAttribute("loggedInUserEmail");
        if (sellerEmail == null) {
            return "redirect:/login";
        }
        List<PNR> pnrs = pnrRepository.findBySellerEmailOrderByJourneyDateAsc(sellerEmail);
        model.addAttribute("pnrs", pnrs);
        return "myListedTickets"; // Name of the template
    }
    
    @GetMapping("/user/transactions")
    public String viewMyTransaction(HttpSession session, Model model) {
        String email = (String) session.getAttribute("loggedInUserEmail"); // or "email" if that's what you store
        if (email == null) return "redirect:/login";

        List<TransactionHistory> list = transactionHistoryRepository.findByUserEmailOrderByDateDesc(email);
        model.addAttribute("transactions", list);
        return "myTransactions"; // this maps to myTransaction.html
    }


    
    @GetMapping("/privacy")
    public String privacy() {
    	return "privacy";
    }
    
    @GetMapping("/terms")
    public String terms() {
    	return "terms";
    }
    
    @GetMapping("/about")
    public String aboutUs() {
    	return "aboutUs";
    }
    
    @GetMapping("/how-it-works")
    public String howTtWorks() {
    	return "howItWorks";
    }
    
    @GetMapping("/faq")
    public String faq() {
    	return "faq";
    }
    
    @GetMapping("/maskedAadhar")
    public String maskedAadhar() {
    	return "maskedAadhar";
    }
    
    @GetMapping("/support/submit")
    public String showSupportForm(Model model, HttpSession session) {
        String email = (String) session.getAttribute("loggedInUserEmail");
        if (email == null) {
            return "redirect:/login";
        }

        model.addAttribute("supportMessage", new SupportMessage());
        List<SupportMessage> messages = supportRepo.findByUserEmailOrderByTimestampAsc(email);
        model.addAttribute("messages", messages);

        return "userSupportForm";
    }

    @PostMapping("/support/submit")
    public String submitSupportMessage(@ModelAttribute SupportMessage supportMessage, HttpSession session) {
        String userEmail = (String) session.getAttribute("loggedInUserEmail");
        if (userEmail == null) {
            return "redirect:/login";
        }

        supportMessage.setUserEmail(userEmail);
        supportMessage.setTimestamp(LocalDateTime.now());
        supportMessage.setResolved(false);
        supportRepo.save(supportMessage);

        return "redirect:/support/submit";
    }
    
    



}