package com.example.SwapTicket.controller;

import com.example.SwapTicket.model.Ticket;
import com.example.SwapTicket.model.User;
import com.example.SwapTicket.repository.TicketRepository;
import com.example.SwapTicket.repository.UserRepository;
import com.example.SwapTicket.service.FileStorageService;

import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/tickets")
public class TicketController {

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private FileStorageService fileStorageService;
    
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private HttpSession session;

    @PostMapping("/sell")
    public String sellTicket(
            @RequestParam("trainNumber") String trainNumber,
            @RequestParam("trainName") String trainName,
            @RequestParam("fromStation") String fromStation,
            @RequestParam("toStation") String toStation,
            @RequestParam("dateOfJourney") String dateOfJourney,
            @RequestParam("classType") String classType,
            @RequestParam("numberOfTickets") Integer numberOfTickets,
            @RequestParam("age") Integer age,
            @RequestParam("pnrNumber") String pnrNumber,
            @RequestParam("passengerNames") String passengerNames,
            @RequestParam("price") Double price,
            @RequestParam("ticketImage") MultipartFile ticketImage,
            RedirectAttributes redirectAttributes,
            Principal principal // To capture seller email
    ) {

        Ticket ticket = new Ticket();
        ticket.setTrainNumber(trainNumber);
        ticket.setTrainName(trainName);
        ticket.setFromStation(fromStation);
        ticket.setToStation(toStation);
        ticket.setDateOfJourney(LocalDate.parse(dateOfJourney));
        ticket.setClassType(classType);
        ticket.setNumberOfTickets(numberOfTickets);
        ticket.setAge(age);
        ticket.setPnrNumber(pnrNumber);
        ticket.setPrice(price);
        ticket.setPassengerNames(Arrays.asList(passengerNames.split("\\s*,\\s*")));
        ticket.setSold(false);

        String sellerEmail = (String) session.getAttribute("loggedInUserEmail");
        ticket.setSellerEmail(sellerEmail); // Set seller info


        if (!ticketImage.isEmpty()) {
            try {
                String fileName = fileStorageService.storeFile(ticketImage);
                ticket.setTicketImagePath(fileName);
            } catch (Exception e) {
                e.printStackTrace();
                redirectAttributes.addFlashAttribute("error", "Failed to upload ticket image.");
                return "redirect:/tickets/sell";
            }
        }

        ticketRepository.save(ticket);
        redirectAttributes.addFlashAttribute("successMessage", "🎉 Ticket submitted successfully!");
        return "redirect:/user/home";
    }

    @GetMapping("/buy")
    public String showFilteredTickets(@RequestParam(required = false) String filter, Model model) {
        List<Ticket> tickets;

        if ("sold".equalsIgnoreCase(filter)) {
            tickets = ticketRepository.findBySold(true);
        } else {
            tickets = ticketRepository.findBySold(false);
            filter = "available";
        }

        model.addAttribute("tickets", tickets);
        model.addAttribute("filter", filter);
        return "buyTickets";
    }

    @PostMapping("/buy/{id}")
    public String buyTicket(@PathVariable Long id, Principal principal) {
        Optional<Ticket> optionalTicket = ticketRepository.findById(id);
        if (optionalTicket.isPresent()) {
            Ticket ticket = optionalTicket.get();
            ticket.setSold(true);

            ticketRepository.save(ticket);
            return "redirect:/tickets/my/" + id;
        }
        return "redirect:/tickets/buy";
    }

    @GetMapping("/my/{id}")
    public String viewMyTicket(@PathVariable Long id, Model model) {
        Optional<Ticket> ticketOptional = ticketRepository.findById(id);
        if (ticketOptional.isPresent()) {
            model.addAttribute("ticket", ticketOptional.get());
            return "myTicket";
        } else {
            return "redirect:/tickets/buy";
        }
    }
    
    @GetMapping("/my-tickets")
    public String showMyTickets(Model model, HttpSession session) {
        String email = (String) session.getAttribute("loggedInUserEmail");
        if (email == null) {
            return "redirect:/login";
        }

        List<Ticket> myTickets = ticketRepository.findBySellerEmail(email);
        System.out.println("Number of tickets: " + myTickets.size()); 
        model.addAttribute("tickets", myTickets);
        return "myTickets";
    }



}
