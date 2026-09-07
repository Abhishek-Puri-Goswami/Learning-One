package com.retailco.bankrag.integration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.retailco.bankrag")
public class BankingSupportServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(BankingSupportServiceApplication.class, args);
    }
}
