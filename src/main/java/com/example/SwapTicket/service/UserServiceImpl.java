package com.example.SwapTicket.service;

import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BindingResult;

import com.example.SwapTicket.helper.EmailSender;
import com.example.SwapTicket.model.User;

import jakarta.servlet.http.HttpSession;

public class UserServiceImpl {
	@Autowired
	EmailSender emailSender;
	public String register(User user, BindingResult result, HttpSession session) {
		if (!user.getPassword().equals(user.getConfirmPassword()))
			result.rejectValue("confirmPassword", "error.confirmPassword",
					"* Password and Confirm password not matching");

		if (result.hasErrors()) {
			return "register.html";
		}

		int otp = new Random().nextInt(100000, 1000000);
		emailSender.sendEmail(user, otp);

		session.setAttribute("otp", otp);
		session.setAttribute("user", user);
		session.setAttribute("pass", "Otp Sent Success");
		return "redirect:/otp";
	}
}
