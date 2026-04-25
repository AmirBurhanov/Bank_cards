package org.example.bank.utils;

import org.springframework.stereotype.Component;

@Component
public class CardNumberMasker {

    public String mask(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 16) {
            return "**** **** **** ****";
        }

        String last4 = cardNumber.substring(cardNumber.length() - 4);
        return "**** **** **** " + last4;
    }
}