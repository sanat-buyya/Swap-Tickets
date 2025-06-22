package com.example.SwapTicket.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.SwapTicket.model.Passenger;

public interface PassengerRepository extends JpaRepository<Passenger, Long> {
	List<Passenger> findBySold(boolean sold);

	List<Passenger> findByBuyerEmail(String email);

	List<Passenger> findBySoldTrueAndSellerPaidFalse();
	
	long count();

	long countBySoldTrue();

	long countBySoldFalse();
	
	List<Passenger> findBySoldTrue();

	Page<Passenger> findBySoldTrue(Pageable pageable);

	List<Passenger> findBySoldTrueAndBuyerEmail(String buyerEmail);

	Passenger getPassengerById(Long passengerId);
	
	boolean existsByBuyerEmailAndSoldTrue(String email);

}


