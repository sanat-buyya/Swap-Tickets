package com.example.SwapTicket.repository;

import com.example.SwapTicket.model.User;
import org.springframework.data.jpa.repository.JpaRepository;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserRepository extends JpaRepository<User, Long> {
    User findByEmail(String email);
    boolean existsByEmail(String email);
    
    Page<User> findAll(Pageable pageable);
}