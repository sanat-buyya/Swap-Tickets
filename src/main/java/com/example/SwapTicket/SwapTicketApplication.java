package com.example.SwapTicket;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SwapTicketApplication {
    public static void main(String[] args) {
        SpringApplication.run(SwapTicketApplication.class, args);
    }
}
