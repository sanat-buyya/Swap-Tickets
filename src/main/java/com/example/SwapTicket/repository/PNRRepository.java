package com.example.SwapTicket.repository;

import com.example.SwapTicket.model.PNR;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface PNRRepository extends JpaRepository<PNR, String> {
    
    List<PNR> findBySellerEmail(String sellerEmail);
    
    List<PNR> findByFromStationAndToStationAndJourneyDate(
        String fromStation, String toStation, 
        java.time.LocalDate journeyDate
    );

	List<PNR> findByTrainNumberAndIsSold(String trainNumber, boolean sold);

	List<PNR> findByIsSold(boolean sold);

	List<PNR> findByTrainNumber(String trainNumber);

	@Query("SELECT p FROM PNR p WHERE LOWER(p.fromStation) LIKE LOWER(CONCAT('%', :from, '%')) AND LOWER(p.toStation) LIKE LOWER(CONCAT('%', :to, '%'))")
	List<PNR> findByFromStationLikeAndToStationLike(@Param("from") String from, @Param("to") String to);
	
	List<PNR> findBySellerEmailOrderByJourneyDateAsc(String sellerEmail);

	List<PNR> findByJourneyDate(LocalDate journeyDate);
	
	List<PNR> findByJourneyDateBefore(LocalDate date);

	


}
