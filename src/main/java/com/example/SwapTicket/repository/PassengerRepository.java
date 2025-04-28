package com.example.SwapTicket.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.SwapTicket.model.Passenger;

public interface PassengerRepository extends JpaRepository<Passenger, Long> {
	List<Passenger> findByTicketPnrNumber(String pnrNumber);
}

