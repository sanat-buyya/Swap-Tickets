package com.example.SwapTicket.repository;

import com.example.SwapTicket.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
    
    List<User> findAllByStatus(String status);
    
    @Modifying
    @Query("UPDATE User u SET u.status = :status WHERE u.id = :id")
    void updateUserStatus(@Param("id") Long id, @Param("status") String status);

}