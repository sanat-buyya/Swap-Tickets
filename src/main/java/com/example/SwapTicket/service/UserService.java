package com.example.SwapTicket.service;

import com.example.SwapTicket.model.User;
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

    
}