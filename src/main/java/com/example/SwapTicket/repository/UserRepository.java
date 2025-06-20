package com.example.SwapTicket.repository;

import com.example.SwapTicket.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserRepository extends JpaRepository<User, Long> {
    User findByEmail(String email);
    boolean existsByEmail(String email);
    
    Page<User> findAll(Pageable pageable);
    
    User findByReferralCode(String referralCode);
    
    List<User> findAllByReferredBy(String referredBy);
    
}