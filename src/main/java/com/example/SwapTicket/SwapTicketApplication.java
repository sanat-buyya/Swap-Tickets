package com.example.SwapTicket;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import com.example.SwapTicket.model.Wallet;
import com.example.SwapTicket.repository.WalletRepository;

@SpringBootApplication
public class SwapTicketApplication {

    public static void main(String[] args) {
        SpringApplication.run(SwapTicketApplication.class, args);
    }

    @Bean
    CommandLineRunner initAdminWallet(WalletRepository walletRepo) {
        return args -> {
            walletRepo.findByEmail("admin@swapticket.com").orElseGet(() -> {
                Wallet adminWallet = new Wallet();
                adminWallet.setEmail("admin@swapticket.com");
                adminWallet.setBalance(10000.0); // Initial balance
                return walletRepo.save(adminWallet);
            });
        };
    }
}
