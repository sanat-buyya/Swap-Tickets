package com.example.SwapTicket.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;


//TransactionHistory.java
@Entity
public class TransactionHistory {

 @Id @GeneratedValue
 private Long id;

 private String userEmail;

 private double amount;

 @Enumerated(EnumType.STRING)
 private TransactionType type;

 private LocalDate date;

public Long getId() {
	return id;
}

public void setId(Long id) {
	this.id = id;
}

public String getUserEmail() {
	return userEmail;
}

public void setUserEmail(String userEmail) {
	this.userEmail = userEmail;
}

public double getAmount() {
	return amount;
}

public void setAmount(double amount) {
	this.amount = amount;
}

public TransactionType getType() {
	return type;
}

public void setType(TransactionType type) {
	this.type = type;
}

public LocalDate getDate() {
	return date;
}

public void setDate(LocalDate date) {
	this.date = date;
}

 
}
