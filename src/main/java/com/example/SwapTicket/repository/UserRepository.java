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
    
    // Existing methods
    User findByEmail(String email);
    boolean existsByEmail(String email);
    Page<User> findAll(Pageable pageable);
    User findByReferralCode(String referralCode);
    List<User> findAllByReferredBy(String referredBy);
    List<User> findAllByStatus(String status);
    
    @Modifying
    @Query("UPDATE User u SET u.status = :status WHERE u.id = :id")
    void updateUserStatus(@Param("id") Long id, @Param("status") String status);
    
    // New search methods for email only
    Page<User> findByEmailContainingIgnoreCase(String email, Pageable pageable);
    Page<User> findByEmailContainingIgnoreCaseAndStatus(String email, String status, Pageable pageable);
    
    // New search methods for status only
    Page<User> findByStatus(String status, Pageable pageable);
    
    // Custom query methods for searching all fields
    @Query("SELECT u FROM User u WHERE " +
           "LOWER(u.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(u.mobile) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<User> findBySearchAll(@Param("search") String search, Pageable pageable);
    
    @Query("SELECT u FROM User u WHERE " +
           "(LOWER(u.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(u.mobile) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
           "u.status = :status")
    Page<User> findBySearchAllAndStatus(@Param("search") String search, @Param("status") String status, Pageable pageable);
    
    // Additional useful methods for admin functionality
    @Query("SELECT COUNT(u) FROM User u WHERE u.status = :status")
    long countByStatus(@Param("status") String status);
    
    @Query("SELECT u FROM User u WHERE u.name LIKE %:name%")
    Page<User> findByNameContainingIgnoreCase(@Param("name") String name, Pageable pageable);
    
    @Query("SELECT u FROM User u WHERE u.mobile LIKE %:mobile%")
    Page<User> findByMobileContaining(@Param("mobile") String mobile, Pageable pageable);
    
    // Combined search with multiple criteria
    @Query("SELECT u FROM User u WHERE " +
           "(:name IS NULL OR LOWER(u.name) LIKE LOWER(CONCAT('%', :name, '%'))) AND " +
           "(:email IS NULL OR LOWER(u.email) LIKE LOWER(CONCAT('%', :email, '%'))) AND " +
           "(:mobile IS NULL OR u.mobile LIKE CONCAT('%', :mobile, '%')) AND " +
           "(:status IS NULL OR u.status = :status)")
    Page<User> findByMultipleCriteria(
        @Param("name") String name,
        @Param("email") String email, 
        @Param("mobile") String mobile,
        @Param("status") String status,
        Pageable pageable
    );
    
    // Get users with referral statistics
    @Query("SELECT u, " +
           "(SELECT COUNT(r) FROM User r WHERE r.referredBy = u.referralCode) as referralCount " +
           "FROM User u")
    Page<Object[]> findAllWithReferralCount(Pageable pageable);
    
    // Search with referral count (for advanced admin features)
    @Query("SELECT u, " +
           "(SELECT COUNT(r) FROM User r WHERE r.referredBy = u.referralCode) as referralCount " +
           "FROM User u WHERE " +
           "(:search IS NULL OR " +
           "LOWER(u.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(u.mobile) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
           "(:status IS NULL OR u.status = :status)")
    Page<Object[]> findAllWithReferralCountAndSearch(
        @Param("search") String search,
        @Param("status") String status,
        Pageable pageable
    );
}
