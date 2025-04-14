package com.example.SwapTicket.repository;

import com.example.SwapTicket.model.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface TicketRepository extends JpaRepository<Ticket, Long> {
    List<Ticket> findBySold(boolean sold);
    
    List<Ticket> findBySellerEmail(String email);
    List<Ticket> findBySellerEmailAndDateOfJourneyAfterOrDateOfJourneyEquals(String email, LocalDate afterDate, LocalDate equalDate);
    List<Ticket> findBySoldFalseAndDateOfJourneyAfterOrderByDateOfJourneyAsc(LocalDate currentDate);
    List<Ticket> findByPnrNumberAndSold(String pnrNumber, boolean sold);
    List<Ticket> findByFromStationIgnoreCaseAndToStationIgnoreCaseAndSold(String fromStation, String toStation, boolean sold);
    List<Ticket> findByBuyerEmail(String buyerEmail);

}
