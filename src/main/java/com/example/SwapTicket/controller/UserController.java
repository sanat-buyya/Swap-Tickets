package com.example.SwapTicket.controller;

import com.example.SwapTicket.model.PNR;
import com.example.SwapTicket.model.Passenger;
import com.example.SwapTicket.model.SupportMessage;
import com.example.SwapTicket.model.TrainResult;
import com.example.SwapTicket.model.TrainStop;
import com.example.SwapTicket.model.TransactionHistory;
import com.example.SwapTicket.model.User;
import com.example.SwapTicket.model.Wallet;
import com.example.SwapTicket.repository.PNRRepository;
import com.example.SwapTicket.repository.PassengerRepository;
import com.example.SwapTicket.repository.SupportMessageRepository;
import com.example.SwapTicket.repository.TransactionHistoryRepository;
import com.example.SwapTicket.repository.UserRepository;
import com.example.SwapTicket.repository.WalletRepository;
import com.example.SwapTicket.service.AdminAuthService;
import com.example.SwapTicket.service.UserService;
import com.example.SwapTicket.helper.AES;
import com.example.SwapTicket.helper.EmailSender;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.HashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
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
    
    @Autowired
    private AdminAuthService adminAuthService;
    
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
                            Model model, HttpSession session) {
        
        if (adminAuthService.authenticateAdmin(username, password)) {
            session.setAttribute("admin", true);
            session.setAttribute("adminEmail", username);
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
    
    @GetMapping("/api/stations")
    @ResponseBody
    public List<Map<String, String>> getAllStations() {
        List<Map<String, String>> stations = new ArrayList<>();
        try {
            Path path = Paths.get("src/main/resources/Train_details_22122017.csv");
            List<String> lines = Files.readAllLines(path);
            for (String line : lines.subList(1, lines.size())) {
                String[] columns = line.split(",", -1);
                if (columns.length >= 5) {
                    String code = columns[3].trim().toUpperCase();
                    String name = columns[4].trim().toUpperCase();
                    Map<String, String> stationMap = new HashMap<>();
                    stationMap.put("code", code);
                    stationMap.put("name", name);
                    stations.add(stationMap);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return stations;
    }
    
 // Step 1: Show Forgot Password Page
    @GetMapping("/forgot-password")
    public String forgotPasswordPage(Model model) {
        model.addAttribute("showOtpForm", null); // Only email form
        return "forgotPassword";
    }

    // Step 2: Send OTP after user submits email
    @PostMapping("/forgot-password")
    public String sendOtpForForgotPassword(@RequestParam("email") String email,
                                           HttpSession session,
                                           Model model) {
        User user = userService.findByEmail(email);
        if (user == null) {
            model.addAttribute("error", "No user found with this email.");
            model.addAttribute("showOtpForm", null);
            return "forgotPassword";
        }

        // Generate and store OTP
        int otp = new Random().nextInt(100000, 1000000);
        emailSender.sendEmail(user, otp); // You should already have this implemented

        // Store in session
        session.setAttribute("forgotUser", user);
        session.setAttribute("forgotOtp", otp);
        session.setAttribute("otpTime", System.currentTimeMillis());

        // Show OTP + new password form
        model.addAttribute("email", email);
        model.addAttribute("showOtpForm", true);
        return "forgotPassword"; // same page, conditionally renders OTP form
    }

    // Step 3: Verify OTP and Reset Password
    @PostMapping("/verify-forgot-password")
    public String verifyForgotOtp(@RequestParam("email") String email,
                                  @RequestParam("otp") int userOtp,
                                  @RequestParam("newPassword") String newPassword,
                                  @RequestParam("confirmPassword") String confirmPassword,
                                  HttpSession session,
                                  Model model) {
        Integer sessionOtp = (Integer) session.getAttribute("forgotOtp");
        User user = (User) session.getAttribute("forgotUser");

        if (sessionOtp == null || user == null || !user.getEmail().equals(email)) {
            model.addAttribute("error", "Session expired or email mismatch. Please try again.");
            model.addAttribute("showOtpForm", null);
            return "forgotPassword";
        }

        if (userOtp != sessionOtp) {
            model.addAttribute("otpError", "Invalid OTP.");
            model.addAttribute("email", email);
            model.addAttribute("showOtpForm", true);
            return "forgotPassword";
        }

        if (!newPassword.equals(confirmPassword)) {
            model.addAttribute("passwordError", "Passwords do not match.");
            model.addAttribute("email", email);
            model.addAttribute("showOtpForm", true);
            return "forgotPassword";
        }

        // Save encrypted password
        String encryptedPassword = AES.encrypt(newPassword);
        user.setPassword(encryptedPassword);
        userService.saveUser(user);

        // Clear session
        session.removeAttribute("forgotOtp");
        session.removeAttribute("forgotUser");
        session.removeAttribute("otpTime");

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
        return "myListedTickets";
    }
    
    @GetMapping("/user/transactions")
    public String viewMyTransaction(HttpSession session, Model model) {
        String email = (String) session.getAttribute("loggedInUserEmail");
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
    
    private final String BASE_URL = "https://api.data.gov.in/resource/13051d52-05c2-4130-9e7b-891bdde84076";
    private final String API_KEY = "579b464db66ec23bdd0000014639b061d7f4430f7e28a3efc8e894b3"; // Replace with your own if needed

    @GetMapping("/train-status")
    public String showTrainStatusForm(HttpSession session) {
    	String userEmail = (String) session.getAttribute("loggedInUserEmail");
        if (userEmail == null) {
            return "redirect:/login";
        }
        return "trainStatus";
    }

    @PostMapping("/train-status")
    public String getTrainStatus(
            @RequestParam(required = false) String trainNo,
            @RequestParam(required = false) String trainName,
            @RequestParam(required = false) String sourceStation,
            @RequestParam(required = false) String destinationStation,
            @RequestParam(required = false) String limit,
            Model model) {

        RestTemplate restTemplate = new RestTemplate();       

        trainNo = normalizeInput(trainNo);
        trainName = normalizeInput(trainName);
        sourceStation = normalizeInput(sourceStation);
        destinationStation = normalizeInput(destinationStation);
        
        StringBuilder urlBuilder = new StringBuilder(BASE_URL);
        urlBuilder.append("?api-key=").append(API_KEY);
        urlBuilder.append("&format=json");

        // Optional filters
        if (limit != null && !limit.isEmpty()) urlBuilder.append("&limit=").append(limit);
        if (trainNo != null && !trainNo.isEmpty()) urlBuilder.append("&filters[train_no]=").append(encode(trainNo));
        if (trainName != null && !trainName.isEmpty()) urlBuilder.append("&filters[train_name]=").append(encode(trainName));
        if (sourceStation != null && !sourceStation.isEmpty()) urlBuilder.append("&filters[source_station]=").append(encode(sourceStation));
        if (destinationStation != null && !destinationStation.isEmpty()) urlBuilder.append("&filters[destination_station]=").append(encode(destinationStation));

        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(urlBuilder.toString(), Map.class);
            Map<String, Object> data = response.getBody();
            
            // Ensure data is not null
            if (data != null) {
                model.addAttribute("trainData", data);
            } else {
                model.addAttribute("error", "No data received from API");
            }
        } catch (Exception e) {
            model.addAttribute("error", "API error: " + e.getMessage());
        }

        return "trainStatus";
    }
    
    private String normalizeInput(String input) {
        if (input == null || input.trim().isEmpty()) {
            return null;
        }
        return input.trim().toUpperCase();
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
    
    public Map<String, List<TrainStop>> parseTrainCsv(String filePath) {
        Map<String, List<TrainStop>> trainRoutes = new HashMap<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String headerLine = br.readLine(); // read header
            String[] headers = headerLine.split(",", -1);

            // Map column names to indices
            Map<String, Integer> columnIndex = new HashMap<>();
            for (int i = 0; i < headers.length; i++) {
                columnIndex.put(headers[i].trim(), i);
            }

            // Ensure required columns exist
            if (!columnIndex.containsKey("Train No") || !columnIndex.containsKey("Train Name") ||
                !columnIndex.containsKey("Station Code") || !columnIndex.containsKey("Arrival time") ||
                !columnIndex.containsKey("Departure Time")) {
                throw new RuntimeException("CSV missing required columns");
            }

            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",", -1);

                try {
                    String trainNo = parts[columnIndex.get("Train No")].trim();
                    String trainName = parts[columnIndex.get("Train Name")].trim();
                    String stationCode = parts[columnIndex.get("Station Code")].trim();
                    String arrivalTime = parts[columnIndex.get("Arrival time")].trim();
                    String departureTime = parts[columnIndex.get("Departure Time")].trim();

                    TrainStop stop = new TrainStop(trainNo, trainName, stationCode, arrivalTime, departureTime);
                    trainRoutes.computeIfAbsent(trainNo, k -> new ArrayList<>()).add(stop);

                } catch (Exception e) {
                    System.err.println("Skipping line due to error: " + line);
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return trainRoutes;
    }

    public List<TrainResult> findMatchingTrains(String start, String end, String csvPath) {
        Map<String, List<TrainStop>> trainRoutes = parseTrainCsv(csvPath);
        List<TrainResult> result = new ArrayList<>();

        for (Map.Entry<String, List<TrainStop>> entry : trainRoutes.entrySet()) {
            List<TrainStop> route = entry.getValue();
            TrainStop sourceStop = null;
            TrainStop destinationStop = null;

            for (TrainStop stop : route) {
                if (stop.getStationCode().equalsIgnoreCase(start)) {
                    sourceStop = stop;
                } else if (stop.getStationCode().equalsIgnoreCase(end)) {
                    destinationStop = stop;
                }
            }

            if (sourceStop != null && destinationStop != null && route.indexOf(sourceStop) < route.indexOf(destinationStop)) {
                TrainResult trainResult = new TrainResult(
                        sourceStop.getTrainNo(),
                        sourceStop.getTrainName(),
                        sourceStop.getStationCode(),
                        destinationStop.getStationCode(),
                        sourceStop.getDepartureTime(),
                        destinationStop.getArrivalTime()
                );
                result.add(trainResult);
            }
        }

        return result;
    }

    @GetMapping("/track-train-between")
    public String trackTrainBetweenStations(HttpSession session) {
    	String userEmail = (String) session.getAttribute("loggedInUserEmail");
        if (userEmail == null) {
            return "redirect:/login";
        }
        return "trainBetweenStations";
    }

    @PostMapping("/track-train-between")
    public String trackTrainBetweenStations(@RequestParam String sourceStation,
                                            @RequestParam String destinationStation,
                                            Model model) {
        String csvPath = "src/main/resources/Train_details_22122017.csv";

        List<TrainResult> matchingTrains = findMatchingTrains(sourceStation, destinationStation, csvPath);

        model.addAttribute("trains", matchingTrains);
        model.addAttribute("sourceStation", sourceStation);
        model.addAttribute("destinationStation", destinationStation);

        return "trainBetweenStations";
    }


}