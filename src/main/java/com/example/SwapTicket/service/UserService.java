package com.example.SwapTicket.service;

import com.example.SwapTicket.model.User;
import com.example.SwapTicket.repository.PassengerRepository;
import com.example.SwapTicket.repository.UserRepository;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    public void saveUser(User user) {
        userRepository.save(user);
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

	public User findByReferralCode(String trim) {
		return userRepository.findByReferralCode(trim);
	}

	public List<User> findAllByReferredBy(String referredBy) {
	    return userRepository.findAllByReferredBy(referredBy);
	}
	
	public User findById(Long id) {
	    return userRepository.findById(id).orElse(null);
	}
	
	// OAuth2 specific methods
	public void saveOAuth2User(User user) {
		// For OAuth2 users, we don't require mobile and DOB validation
		userRepository.save(user);
	}
	
	public void updateUser(User user) {
		userRepository.save(user);
	}
	
	public List<User> findAllUsers() {
	    return userRepository.findAll();
	}
}