package com.example.SwapTicket.repository;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.example.SwapTicket.model.Passenger;

public interface PassengerRepository extends JpaRepository<Passenger, Long> {
    
    // Existing methods
    List<Passenger> findBySold(boolean sold);
    List<Passenger> findByBuyerEmail(String email);
    List<Passenger> findBySoldTrueAndSellerPaidFalse();
    long count();
    long countBySoldTrue();
    long countBySoldFalse();
    List<Passenger> findBySoldTrue();
    Page<Passenger> findBySoldTrue(Pageable pageable);
    List<Passenger> findBySoldTrueAndBuyerEmail(String buyerEmail);
    Passenger getPassengerById(Long passengerId);
    boolean existsByBuyerEmailAndSoldTrue(String email);

    // Existing search methods for admin dashboard filtering
    @Query("SELECT p FROM Passenger p JOIN p.pnr pnr WHERE p.sold = true AND p.sellerPaid = false AND " +
           "(LOWER(pnr.pnrNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(pnr.trainNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.buyerEmail) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "pnr.sellerMobile LIKE CONCAT('%', :search, '%'))")
    List<Passenger> findBySoldTrueAndSellerPaidFalseAndSearchAll(@Param("search") String search);

    @Query("SELECT p FROM Passenger p JOIN p.pnr pnr WHERE p.sold = true AND p.sellerPaid = false AND " +
           "LOWER(pnr.pnrNumber) LIKE LOWER(CONCAT('%', :search, '%'))")
    List<Passenger> findBySoldTrueAndSellerPaidFalseAndPnrContaining(@Param("search") String search);

    @Query("SELECT p FROM Passenger p JOIN p.pnr pnr WHERE p.sold = true AND p.sellerPaid = false AND " +
           "LOWER(pnr.trainNumber) LIKE LOWER(CONCAT('%', :search, '%'))")
    List<Passenger> findBySoldTrueAndSellerPaidFalseAndTrainContaining(@Param("search") String search);

    @Query("SELECT p FROM Passenger p WHERE p.sold = true AND p.sellerPaid = false AND " +
           "LOWER(p.buyerEmail) LIKE LOWER(CONCAT('%', :search, '%'))")
    List<Passenger> findBySoldTrueAndSellerPaidFalseAndBuyerEmailContaining(@Param("search") String search);

    @Query("SELECT p FROM Passenger p JOIN p.pnr pnr WHERE p.sold = true AND p.sellerPaid = false AND " +
           "pnr.sellerMobile LIKE CONCAT('%', :search, '%')")
    List<Passenger> findBySoldTrueAndSellerPaidFalseAndSellerMobileContaining(@Param("search") String search);

    // Additional useful search methods for other admin functionalities
    @Query("SELECT p FROM Passenger p JOIN p.pnr pnr WHERE " +
           "(LOWER(pnr.pnrNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(pnr.trainNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.buyerEmail) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "pnr.sellerMobile LIKE CONCAT('%', :search, '%'))")
    List<Passenger> findBySearchAll(@Param("search") String search);

    @Query("SELECT p FROM Passenger p JOIN p.pnr pnr WHERE p.sold = true AND " +
           "(LOWER(pnr.pnrNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(pnr.trainNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.buyerEmail) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "pnr.sellerMobile LIKE CONCAT('%', :search, '%'))")
    List<Passenger> findBySoldTrueAndSearchAll(@Param("search") String search);

    // ===== NEW METHODS FOR TICKET MANAGEMENT PAGE =====
    
    // Status-only filtering with pagination
    Page<Passenger> findBySold(Boolean sold, Pageable pageable);
    
    // PNR search methods with pagination
    Page<Passenger> findByPnrPnrNumberContainingIgnoreCase(String pnrNumber, Pageable pageable);
    Page<Passenger> findByPnrPnrNumberContainingIgnoreCaseAndSold(String pnrNumber, Boolean sold, Pageable pageable);
    long countByPnrPnrNumberContainingIgnoreCaseAndSold(String pnrNumber, Boolean sold);
    
    // Passenger name search methods with pagination
    Page<Passenger> findByNameContainingIgnoreCase(String name, Pageable pageable);
    Page<Passenger> findByNameContainingIgnoreCaseAndSold(String name, Boolean sold, Pageable pageable);
    long countByNameContainingIgnoreCaseAndSold(String name, Boolean sold);
    
    // Seller email search methods with pagination
    Page<Passenger> findBySellerEmailContainingIgnoreCase(String sellerEmail, Pageable pageable);
    Page<Passenger> findBySellerEmailContainingIgnoreCaseAndSold(String sellerEmail, Boolean sold, Pageable pageable);
    long countBySellerEmailContainingIgnoreCaseAndSold(String sellerEmail, Boolean sold);
    
    // Search all fields with pagination
    @Query("SELECT p FROM Passenger p JOIN p.pnr pnr WHERE " +
           "(LOWER(pnr.pnrNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.sellerEmail) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.buyerEmail) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(pnr.trainNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.coach) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.seat) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Passenger> findBySearchAll(@Param("search") String search, Pageable pageable);
    
    // Search all fields with status filter and pagination
    @Query("SELECT p FROM Passenger p JOIN p.pnr pnr WHERE p.sold = :sold AND " +
           "(LOWER(pnr.pnrNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.sellerEmail) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.buyerEmail) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(pnr.trainNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.coach) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.seat) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Passenger> findBySearchAllAndSold(@Param("search") String search, @Param("sold") Boolean sold, Pageable pageable);
    
    // Count methods for search all fields with status
    @Query("SELECT COUNT(p) FROM Passenger p JOIN p.pnr pnr WHERE p.sold = :sold AND " +
           "(LOWER(pnr.pnrNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.sellerEmail) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.buyerEmail) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(pnr.trainNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.coach) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.seat) LIKE LOWER(CONCAT('%', :search, '%')))")
    long countBySearchAllAndSold(@Param("search") String search, @Param("sold") Boolean sold);
    
    // Additional helper methods for buyer email search
    Page<Passenger> findByBuyerEmailContainingIgnoreCase(String buyerEmail, Pageable pageable);
    Page<Passenger> findByBuyerEmailContainingIgnoreCaseAndSold(String buyerEmail, Boolean sold, Pageable pageable);
    long countByBuyerEmailContainingIgnoreCaseAndSold(String buyerEmail, Boolean sold);
    
    // Coach and seat search methods
    Page<Passenger> findByCoachContainingIgnoreCase(String coach, Pageable pageable);
    Page<Passenger> findByCoachContainingIgnoreCaseAndSold(String coach, Boolean sold, Pageable pageable);
    
    Page<Passenger> findBySeatContainingIgnoreCase(String seat, Pageable pageable);
    Page<Passenger> findBySeatContainingIgnoreCaseAndSold(String seat, Boolean sold, Pageable pageable);
    
    // Train number search methods (through PNR relationship)
    @Query("SELECT p FROM Passenger p JOIN p.pnr pnr WHERE " +
           "LOWER(pnr.trainNumber) LIKE LOWER(CONCAT('%', :trainNumber, '%'))")
    Page<Passenger> findByTrainNumberContainingIgnoreCase(@Param("trainNumber") String trainNumber, Pageable pageable);
    
    @Query("SELECT p FROM Passenger p JOIN p.pnr pnr WHERE p.sold = :sold AND " +
           "LOWER(pnr.trainNumber) LIKE LOWER(CONCAT('%', :trainNumber, '%'))")
    Page<Passenger> findByTrainNumberContainingIgnoreCaseAndSold(@Param("trainNumber") String trainNumber, @Param("sold") Boolean sold, Pageable pageable);
    
    @Query("SELECT COUNT(p) FROM Passenger p JOIN p.pnr pnr WHERE p.sold = :sold AND " +
           "LOWER(pnr.trainNumber) LIKE LOWER(CONCAT('%', :trainNumber, '%'))")
    long countByTrainNumberContainingIgnoreCaseAndSold(@Param("trainNumber") String trainNumber, @Param("sold") Boolean sold);

    // ===== ADDITIONAL METHODS FOR TRANSACTION SEARCH =====
    
    // Buyer email search methods for transactions (List versions)
    List<Passenger> findByBuyerEmailContainingIgnoreCaseAndSold(String buyerEmail, Boolean sold);
    
    // Payment ID search methods for transactions
    List<Passenger> findByRazorpayPaymentIdContainingIgnoreCaseAndSold(String paymentId, Boolean sold);
    Page<Passenger> findByRazorpayPaymentIdContainingIgnoreCaseAndSold(String paymentId, Boolean sold, Pageable pageable);
    
    // Enhanced search all fields for transactions (includes payment ID and razorpay fields)
    @Query("SELECT p FROM Passenger p JOIN p.pnr pnr WHERE p.sold = true AND " +
           "(LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.buyerEmail) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.sellerEmail) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.razorpayPaymentId) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.razorpayOrderId) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(pnr.pnrNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(pnr.trainNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.coach) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.seat) LIKE LOWER(CONCAT('%', :search, '%')))")
    List<Passenger> findBySoldTrueAndSearchAllTransactions(@Param("search") String search);
    
    @Query("SELECT p FROM Passenger p JOIN p.pnr pnr WHERE p.sold = true AND " +
           "(LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.buyerEmail) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.sellerEmail) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.razorpayPaymentId) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.razorpayOrderId) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(pnr.pnrNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(pnr.trainNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.coach) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.seat) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Passenger> findBySoldTrueAndSearchAllTransactions(@Param("search") String search, Pageable pageable);
    
    // Seller email search methods (List versions for transactions)
    List<Passenger> findBySellerEmailContainingIgnoreCaseAndSold(String sellerEmail, Boolean sold);
    
    // Name search methods (List versions for transactions)
    List<Passenger> findByNameContainingIgnoreCaseAndSold(String name, Boolean sold);
    
    // Revenue calculation methods - Using native queries for better database compatibility
    @Query("SELECT SUM(p.adminFee) FROM Passenger p WHERE p.sold = true")
    Double calculateTotalRevenue();
    
    @Query("SELECT SUM(p.adminFee) FROM Passenger p WHERE p.sold = true AND " +
           "LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%'))")
    Double calculateRevenueByNameSearch(@Param("search") String search);
    
    @Query("SELECT SUM(p.adminFee) FROM Passenger p WHERE p.sold = true AND " +
           "LOWER(p.buyerEmail) LIKE LOWER(CONCAT('%', :search, '%'))")
    Double calculateRevenueByBuyerEmailSearch(@Param("search") String search);
    
    @Query("SELECT SUM(p.adminFee) FROM Passenger p WHERE p.sold = true AND " +
           "LOWER(p.sellerEmail) LIKE LOWER(CONCAT('%', :search, '%'))")
    Double calculateRevenueBySellerEmailSearch(@Param("search") String search);
    
    @Query("SELECT SUM(p.adminFee) FROM Passenger p WHERE p.sold = true AND " +
           "LOWER(p.razorpayPaymentId) LIKE LOWER(CONCAT('%', :search, '%'))")
    Double calculateRevenueByPaymentIdSearch(@Param("search") String search);
    
    @Query("SELECT SUM(p.adminFee) FROM Passenger p JOIN p.pnr pnr WHERE p.sold = true AND " +
           "(LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.buyerEmail) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.sellerEmail) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.razorpayPaymentId) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.razorpayOrderId) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(pnr.pnrNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(pnr.trainNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.coach) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.seat) LIKE LOWER(CONCAT('%', :search, '%')))")
    Double calculateRevenueBySearchAllTransactions(@Param("search") String search);
    
    // Date-based filtering methods using native SQL for better compatibility
    @Query(value = "SELECT * FROM passenger WHERE sold = true AND DATE(created_at) = CURDATE()", 
           nativeQuery = true)
    List<Passenger> findTodaysTransactions();
    
    @Query(value = "SELECT * FROM passenger WHERE sold = true AND created_at >= DATE_SUB(CURDATE(), INTERVAL 7 DAY)", 
           nativeQuery = true)
    List<Passenger> findWeeklyTransactions();
    
    @Query(value = "SELECT * FROM passenger WHERE sold = true AND MONTH(created_at) = MONTH(CURDATE()) AND YEAR(created_at) = YEAR(CURDATE())", 
           nativeQuery = true)
    List<Passenger> findMonthlyTransactions();
    
    // Revenue calculation for date ranges using native SQL
    @Query(value = "SELECT SUM(admin_fee) FROM passenger WHERE sold = true AND DATE(created_at) = CURDATE()", 
           nativeQuery = true)
    Double calculateTodaysRevenue();
    
    @Query(value = "SELECT SUM(admin_fee) FROM passenger WHERE sold = true AND created_at >= DATE_SUB(CURDATE(), INTERVAL 7 DAY)", 
           nativeQuery = true)
    Double calculateWeeklyRevenue();
    
    @Query(value = "SELECT SUM(admin_fee) FROM passenger WHERE sold = true AND MONTH(created_at) = MONTH(CURDATE()) AND YEAR(created_at) = YEAR(CURDATE())", 
           nativeQuery = true)
    Double calculateMonthlyRevenue();
    
    // Count methods for transaction filtering
    @Query("SELECT COUNT(p) FROM Passenger p WHERE p.sold = true AND " +
           "LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%'))")
    long countByNameContainingIgnoreCaseAndSoldTrue(@Param("search") String search);
    
    @Query("SELECT COUNT(p) FROM Passenger p WHERE p.sold = true AND " +
           "LOWER(p.buyerEmail) LIKE LOWER(CONCAT('%', :search, '%'))")
    long countByBuyerEmailContainingIgnoreCaseAndSoldTrue(@Param("search") String search);
    
    @Query("SELECT COUNT(p) FROM Passenger p WHERE p.sold = true AND " +
           "LOWER(p.sellerEmail) LIKE LOWER(CONCAT('%', :search, '%'))")
    long countBySellerEmailContainingIgnoreCaseAndSoldTrue(@Param("search") String search);
    
    @Query("SELECT COUNT(p) FROM Passenger p WHERE p.sold = true AND " +
           "LOWER(p.razorpayPaymentId) LIKE LOWER(CONCAT('%', :search, '%'))")
    long countByRazorpayPaymentIdContainingIgnoreCaseAndSoldTrue(@Param("search") String search);
    
    // Count method for enhanced transaction search
    @Query("SELECT COUNT(p) FROM Passenger p JOIN p.pnr pnr WHERE p.sold = true AND " +
           "(LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.buyerEmail) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.sellerEmail) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.razorpayPaymentId) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.razorpayOrderId) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(pnr.pnrNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(pnr.trainNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.coach) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.seat) LIKE LOWER(CONCAT('%', :search, '%')))")
    long countBySoldTrueAndSearchAllTransactions(@Param("search") String search);
    
    // Additional utility methods for statistics
    @Query("SELECT COUNT(p) FROM Passenger p WHERE p.sold = true")
    long countTotalTransactions();
    
    @Query(value = "SELECT COUNT(*) FROM passenger WHERE sold = true AND DATE(created_at) = CURDATE()", 
           nativeQuery = true)
    long countTodaysTransactions();
    
    @Query(value = "SELECT COUNT(*) FROM passenger WHERE sold = true AND created_at >= DATE_SUB(CURDATE(), INTERVAL 7 DAY)", 
           nativeQuery = true)
    long countWeeklyTransactions();
    
    @Query(value = "SELECT COUNT(*) FROM passenger WHERE sold = true AND MONTH(created_at) = MONTH(CURDATE()) AND YEAR(created_at) = YEAR(CURDATE())", 
           nativeQuery = true)
    long countMonthlyTransactions();
    
    // Methods for finding transactions by date range (using parameters for flexibility)
    @Query(value = "SELECT * FROM passenger WHERE sold = true AND DATE(created_at) BETWEEN ?1 AND ?2", 
           nativeQuery = true)
    List<Passenger> findTransactionsByDateRange(String startDate, String endDate);
    
    @Query(value = "SELECT SUM(admin_fee) FROM passenger WHERE sold = true AND DATE(created_at) BETWEEN ?1 AND ?2", 
           nativeQuery = true)
    Double calculateRevenueByDateRange(String startDate, String endDate);
    
    @Query(value = "SELECT COUNT(*) FROM passenger WHERE sold = true AND DATE(created_at) BETWEEN ?1 AND ?2", 
           nativeQuery = true)
    long countTransactionsByDateRange(String startDate, String endDate);
}
