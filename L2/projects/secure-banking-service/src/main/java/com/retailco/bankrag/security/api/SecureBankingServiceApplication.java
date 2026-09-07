package com.retailco.bankrag.security.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.retailco.bankrag.security")
public class SecureBankingServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(SecureBankingServiceApplication.class, args);
    }
}
