package org.example.bank;

import org.springframework.boot.SpringApplication;

public class TestBankCardsApplication {

    public static void main(String[] args) {
        SpringApplication.from(BankCardsApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
