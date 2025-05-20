package com.example.SwapTicket.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.example.SwapTicket.model.SupportMessage;

public interface SupportMessageRepository extends JpaRepository<SupportMessage, Long> {
    List<SupportMessage> findByResolvedFalse();
    List<SupportMessage> findByUserEmail(String email);
	List<SupportMessage> findAllByOrderByTimestampDesc();
	List<SupportMessage> findByUserEmailOrderByTimestampDesc(String email);
	
	@Query("SELECT DISTINCT s.userEmail FROM SupportMessage s")
	List<String> findDistinctUserEmails();

	List<SupportMessage> findByUserEmailOrderByTimestampAsc(String email);
	void deleteByUserEmail(String email);

}
