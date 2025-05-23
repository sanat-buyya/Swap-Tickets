package com.example.SwapTicket.helper;


import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.example.SwapTicket.model.PNR;
import com.example.SwapTicket.model.Passenger;
import com.example.SwapTicket.model.User;

import jakarta.mail.internet.MimeMessage;

@Component
public class EmailSender {

	@Autowired
	JavaMailSender mailSender;

	@Autowired
	TemplateEngine engine;

	@Value("${spring.mail.username}")
	String from;

	public void sendEmail(User userDto, int otp) {

		Context context = new Context();
		context.setVariable("otp", otp);
		context.setVariable("name", userDto.getName());

		String text = engine.process("email-template.html", context);

		try {
			MimeMessage message = mailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(message);

			helper.setFrom(from, "SwapTicket Application");
			helper.setTo(userDto.getEmail());
			helper.setSubject("OTP verification");
			helper.setText(text, true);

			mailSender.send(message);
		} catch (Exception e) {
			System.err.println("OTP Sending to Email Failed but the otp is : " + otp);
		}
	}
	
	public void sendSoldMessage(User seller, PNR pnr, List<Passenger> soldPassengers) {
	    Context context = new Context();
	    context.setVariable("name", seller.getName());
	    context.setVariable("pnr", pnr.getPnrNumber());
	    context.setVariable("train", pnr.getTrainNumber());
	    context.setVariable("date", pnr.getJourneyDate());
	    
	    List<String> names = soldPassengers.stream()
	        .map(Passenger::getName)
	        .collect(Collectors.toList());

	    context.setVariable("passengerNames", names);

	    String text = engine.process("ticket-sold-template.html", context);

	    try {
	        MimeMessage message = mailSender.createMimeMessage();
	        MimeMessageHelper helper = new MimeMessageHelper(message);
	        helper.setFrom(from, "SwapTicket Application");
	        helper.setTo(seller.getEmail());
	        helper.setSubject("Your ticket has been sold!");
	        helper.setText(text, true);
	        mailSender.send(message);
	    } catch (Exception e) {
	        System.err.println("Ticket sold email failed to send.");
	    }
	}

	public void sendPayoutMessage(User seller, Passenger passenger) {
	    Context context = new Context();
	    context.setVariable("name", seller.getName());
	    context.setVariable("passengerName", passenger.getName());
	    context.setVariable("amount", passenger.getPrice());

	    String htmlContent = engine.process("payout-success-template.html", context);

	    try {
	        MimeMessage message = mailSender.createMimeMessage();
	        MimeMessageHelper helper = new MimeMessageHelper(message);

	        helper.setFrom(from, "SwapTicket Application");
	        helper.setTo(seller.getEmail());
	        helper.setSubject("Amount Credited to Your Wallet");
	        helper.setText(htmlContent, true);

	        mailSender.send(message);
	    } catch (Exception e) {
	        System.err.println("Payout email failed: " + e.getMessage());
	    }
	}


}
