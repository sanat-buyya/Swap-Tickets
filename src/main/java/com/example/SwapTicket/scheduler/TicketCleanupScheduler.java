package com.example.SwapTicket.scheduler;

import com.example.SwapTicket.repository.PNRRepository;
import com.example.SwapTicket.model.PNR;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
public class TicketCleanupScheduler {

    @Autowired
    private PNRRepository pnrRepository;

    @Scheduled(cron = "0 0 2 * * *") // Every day at 2 AM
    public void deleteExpiredTickets() {
        LocalDate cutoffDate = LocalDate.now().minusDays(2);
        List<PNR> expiredPNRs = pnrRepository.findByJourneyDateBefore(cutoffDate);

        if (!expiredPNRs.isEmpty()) {
            pnrRepository.deleteAll(expiredPNRs);
            System.out.println("Deleted " + expiredPNRs.size() + " expired PNR tickets.");
        }
    }
}
