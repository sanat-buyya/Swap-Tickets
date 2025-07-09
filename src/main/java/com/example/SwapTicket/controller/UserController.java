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
import com.example.SwapTicket.repository.UserRepository;
import com.example.SwapTicket.repository.WalletRepository;
import com.example.SwapTicket.service.UserService;
import com.example.SwapTicket.helper.AES;
import com.example.SwapTicket.helper.EmailSender;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional; // Replace with your actual package
import java.util.Random;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class UserController {
	
	@Value("${admin.email}")
	String adminEmail;

	@Value("${admin.password}")
	String adminPassword;

    @Autowired
    private WalletRepository walletRepository;
    
    @Autowired
    private UserRepository userRepository;
    
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
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
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
    public String showRegisterPage(@RequestParam(value = "ref", required = false) String referralCode,
                                   Model model) {
        User user = new User();

        if (referralCode != null && !referralCode.isBlank()) {
            user.setReferredBy(referralCode.trim());
        }

        model.addAttribute("user", user);
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(@Valid @ModelAttribute("user") User user,
                               BindingResult result,
                               Model model,
                               HttpSession session,
                               @RequestParam(value = "referredBy", required = false) String referredBy) {

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

        // ✅ Validate referral code, if provided
        if (referredBy != null && !referredBy.trim().isBlank()) {
            User referrer = userService.findByReferralCode(referredBy.trim());
            if (referrer == null) {
                model.addAttribute("referralError", "Invalid referral code");
                return "register";
            }
            user.setReferredBy(referredBy.trim()); 
        } else {
            user.setReferredBy(null); 
        }

        int otp = new Random().nextInt(100000, 1000000);
        emailSender.sendEmail(user, otp);

        session.setAttribute("otp", otp);
        session.setAttribute("userDto", user); 
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
            user.setConfirmPassword(encryptedPassword);

            // ✅ Generate unique referral code
            String referralCode = user.getEmail().substring(0, 4).toUpperCase()
                    + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
            user.setReferralCode(referralCode);

            userService.saveUser(user); // includes referredBy if valid

            // Create wallet
            Wallet wallet = new Wallet();
            wallet.setEmail(user.getEmail());
            wallet.setBalance(0.0);
            walletRepository.save(wallet);

            // Clear session
            session.removeAttribute("otp");
            session.removeAttribute("userDto");

            return "redirect:/login";
        } else {
            model.addAttribute("otpError", "Invalid OTP. Please try again.");
            return "otp";
        }
    }

    @GetMapping("/user/sell")
    public String sellTicket(HttpSession session, RedirectAttributes redirectAttributes) {
        String email = (String) session.getAttribute("loggedInUserEmail");

        if (email == null) {
            return "redirect:/login";
        }

        User user = userService.findByEmail(email);

        if (user == null || "BLOCKED".equalsIgnoreCase(user.getStatus())) {
            redirectAttributes.addFlashAttribute("error", "Access denied. Your account is blocked.");
            return "redirect:/user/home";
        }

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
    
    @GetMapping("/refund")
    public String refund() {
    	return "refundpolicy";
    }
    
    @GetMapping("/maskedAadhar")
    public String maskedAadhar() {
    	return "maskedAadhar";
    }
    
    @GetMapping("/support/faq")
    public String showSupportFaq(HttpSession session) {
    	String email = (String) session.getAttribute("loggedInUserEmail");
        if (email == null) {
            return "redirect:/login";
        }
    	return "supportFaq";
    }
 
    // Support-related methods
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

    @GetMapping("/support/messages")
    @ResponseBody
    public List<SupportMessage> getMessages(HttpSession session) {
        String email = (String) session.getAttribute("loggedInUserEmail");
        if (email == null) {
            return new ArrayList<>();
        }
        return supportRepo.findByUserEmailOrderByTimestampAsc(email);
    }

    @PostMapping("/support/submit")
    @ResponseBody
    public ResponseEntity<Void> submitSupportMessage(@RequestBody SupportMessage supportMessage, HttpSession session) {
        String userEmail = (String) session.getAttribute("loggedInUserEmail");
        if (userEmail == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        supportMessage.setUserEmail(userEmail);
        supportMessage.setTimestamp(LocalDateTime.now());
        supportMessage.setResolved(false);
        
        SupportMessage savedMessage = supportRepo.save(supportMessage);
        
        // Broadcast to admin channel for this user
        messagingTemplate.convertAndSend("/topic/admin/" + userEmail, savedMessage);
        
        return ResponseEntity.ok().build();
    }

//    // WebSocket message handlers
//    @MessageMapping("/support/user")
//    public void handleUserMessage(@Payload SupportMessage message) {
//        // Set timestamp and save user message
//        message.setTimestamp(LocalDateTime.now());
//        message.setResolved(false);
//        
//        SupportMessage savedMessage = supportRepo.save(message);
//        
//        // Broadcast to admin channel for this user
//        messagingTemplate.convertAndSend("/topic/admin/" + message.getUserEmail(), savedMessage);
//    }

    @MessageMapping("/support/admin")
    public void handleAdminReply(@Payload SupportMessage message) {
        
    	message.setTimestamp(LocalDateTime.now());
        message.setResolved(true);
        
        SupportMessage savedMessage = supportRepo.save(message);
        
        // Broadcast to user channel
        messagingTemplate.convertAndSend("/topic/user/" + message.getUserEmail(), savedMessage);
        
        // Also broadcast to admin channel to update admin's view
        messagingTemplate.convertAndSend("/topic/admin/" + message.getUserEmail(), savedMessage);
    }

    // Legacy WebSocket handler (keeping for compatibility)
    @MessageMapping("/support")
    public void handleSupportMessage(SupportMessage message) {
        
    	message.setTimestamp(LocalDateTime.now());
        message.setResolved(false);
        
        SupportMessage savedMessage = supportRepo.save(message);
        
        // Broadcast to admin channel
        messagingTemplate.convertAndSend("/topic/admin/" + message.getUserEmail(), savedMessage);
    }

    public String generateReferralCode(String nameOrEmail) {
        return nameOrEmail.substring(0, 4).toUpperCase() + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
    }
    
    @GetMapping("/user/refer")
    public String referPage(Model model, HttpSession session) {
        String userEmail = (String) session.getAttribute("loggedInUserEmail");
        if (userEmail == null) {
            return "redirect:/login";
        }

        User user = userRepository.findByEmail(userEmail);
        String referralCode = user.getReferralCode();

        String baseUrl = "http://localhost:8085/register"; 
        String referralLink = baseUrl + "?ref=" + referralCode;

        model.addAttribute("referralCode", referralCode);
        model.addAttribute("referralLink", referralLink);

        List<User> referredUsers = userRepository.findAllByReferredBy(referralCode);
        model.addAttribute("referredUsers", referredUsers);

        return "refer";
    }

   

}