package com.example.SwapTicket.scheduler;

import com.example.SwapTicket.repository.PNRRepository;
import com.example.SwapTicket.model.PNR;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Component
public class TicketCleanupScheduler {

    private static final Logger logger = LoggerFactory.getLogger(TicketCleanupScheduler.class);

    @Autowired
    private PNRRepository pnrRepository;

    @Scheduled(cron = "0 0 2 * * *") // Every day at 2 AM
    @Transactional
    public void deleteExpiredTickets() {
        try {
            logger.info("Starting expired ticket cleanup process...");
            
            LocalDate cutoffDate = LocalDate.now().minusDays(2);
            List<PNR> expiredPNRs = pnrRepository.findByJourneyDateBefore(cutoffDate);

            if (!expiredPNRs.isEmpty()) {
                logger.info("Found {} expired PNR tickets to delete", expiredPNRs.size());
                
                // Delete in batches to avoid memory issues
                int batchSize = 100;
                int totalDeleted = 0;
                
                for (int i = 0; i < expiredPNRs.size(); i += batchSize) {
                    int endIndex = Math.min(i + batchSize, expiredPNRs.size());
                    List<PNR> batch = expiredPNRs.subList(i, endIndex);
                    
                    try {
                        pnrRepository.deleteAll(batch);
                        totalDeleted += batch.size();
                        logger.debug("Deleted batch of {} expired PNR tickets", batch.size());
                    } catch (Exception e) {
                        logger.error("Failed to delete batch of expired PNR tickets: {}", e.getMessage(), e);
                        // Continue with next batch
                    }
                }
                
                logger.info("Successfully deleted {} expired PNR tickets", totalDeleted);
            } else {
                logger.info("No expired PNR tickets found for cleanup");
            }
            
        } catch (Exception e) {
            logger.error("Error during expired ticket cleanup: {}", e.getMessage(), e);
        }
    }
}
