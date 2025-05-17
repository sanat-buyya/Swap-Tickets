package com.example.SwapTicket.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.SwapTicket.model.TransactionHistory;


public interface TransactionHistoryRepository extends JpaRepository<TransactionHistory, Long> {

	List<TransactionHistory> findByUserEmailOrderByDateDesc(String userEmail);
}
