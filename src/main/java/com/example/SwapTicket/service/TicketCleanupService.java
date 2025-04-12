package com.example.SwapTicket.service;

import com.example.SwapTicket.model.Ticket;
import com.example.SwapTicket.repository.TicketRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class TicketCleanupService {

    @Autowired
    private TicketRepository ticketRepository;

    // Runs every day at 2:00 AM
    @Scheduled(cron = "0 0 2 * * ?")
    public void deleteExpiredTickets() {
        LocalDate today = LocalDate.now();
        List<Ticket> tickets = ticketRepository.findAll();
        for (Ticket ticket : tickets) {
            if (ticket.getDateOfJourney().isBefore(today)) {
                ticketRepository.delete(ticket);
            }
        }
        System.out.println("✅ Old tickets cleaned up on " + today);
    }
}
