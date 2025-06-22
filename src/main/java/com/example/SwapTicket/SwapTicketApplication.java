package com.example.SwapTicket;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import com.example.SwapTicket.model.Wallet;
import com.example.SwapTicket.repository.WalletRepository;

@EnableScheduling
@SpringBootApplication
public class SwapTicketApplication {
	
	@Value("${admin.email}")
	String adminEmail;
	
    public static void main(String[] args) {
        SpringApplication.run(SwapTicketApplication.class, args);
    }

    @Bean
    CommandLineRunner initAdminWallet(WalletRepository walletRepo) {
        return args -> {
            walletRepo.findByEmail(adminEmail).orElseGet(() -> {
            	Wallet adminWallet = new Wallet();
                adminWallet.setEmail(adminEmail);
                adminWallet.setBalance(10000.0); // Initial balance
                return walletRepo.save(adminWallet);
            });
        };
    }
}
