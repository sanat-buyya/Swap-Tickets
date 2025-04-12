package com.example.SwapTicket.repository;

import com.example.SwapTicket.model.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TicketRepository extends JpaRepository<Ticket, Long> {
    List<Ticket> findBySold(boolean sold);
    
    List<Ticket> findBySellerEmail(String email);

}
