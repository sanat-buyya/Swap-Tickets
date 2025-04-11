package com.example.SwapTicket.controller;

import com.example.SwapTicket.model.Ticket;
import com.example.SwapTicket.repository.TicketRepository;
import com.example.SwapTicket.service.FileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
            Model model) {

        Ticket ticket = new Ticket();

        ticket.setTrainNumber(trainNumber);
        ticket.setTrainName(trainName);
        ticket.setFromStation(fromStation);
        ticket.setToStation(toStation);
        ticket.setDateOfJourney(LocalDate.parse(dateOfJourney));
        ticket.setClassType(classType);
        ticket.setNumberOfTickets(numberOfTickets); // 👈 set number of tickets
        ticket.setAge(age);                         // 👈 set age
        ticket.setPnrNumber(pnrNumber);
        ticket.setPrice(price);
        ticket.setPassengerNames(Arrays.asList(passengerNames.split("\\s*,\\s*")));

        if (!ticketImage.isEmpty()) {
            try {
                String fileName = fileStorageService.storeFile(ticketImage);
                ticket.setTicketImagePath(fileName);
            } catch (Exception e) {
                e.printStackTrace();
                model.addAttribute("error", "Failed to upload ticket image.");
                return "sellTicket";
            }
        }

        ticketRepository.save(ticket);
        model.addAttribute("success", "Ticket submitted successfully!");
        return "userHome";
    }
    
    @GetMapping("/buy")
    public String showAvailableTickets(Model model) {
        List<Ticket> tickets = ticketRepository.findAll();
        model.addAttribute("tickets", tickets);
        return "buyTickets"; // View to show all tickets
    }

    @PostMapping("/buy/{id}")
    public String buyTicket(@PathVariable Long id, Model model) {
        Optional<Ticket> optionalTicket = ticketRepository.findById(id);
        if (optionalTicket.isPresent()) {
            Ticket ticket = optionalTicket.get();
            ticket.setSold(true); // Mark as sold
            ticketRepository.save(ticket);
        }
        return "redirect:/tickets/buy"; // Refresh the list
    }

   

   

}
