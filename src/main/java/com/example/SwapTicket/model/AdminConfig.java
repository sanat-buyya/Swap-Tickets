package com.example.SwapTicket.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class AdminConfig {
    @Id
    private Long id = 1L;

    private double bookingFee;  // Extra charge per passenger

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public double getBookingFee() {
		return bookingFee;
	}

	public void setBookingFee(double bookingFee) {
		this.bookingFee = bookingFee;
	}
    
    
}


