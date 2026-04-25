package org.example.bank.utils;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CardNumberEncryptor {

    private final TextEncryptor textEncryptor;

    public String encrypt(String cardNumber) {
        return textEncryptor.encrypt(cardNumber);
    }

    public String decrypt(String encryptedCardNumber) {
        return textEncryptor.decrypt(encryptedCardNumber);
    }
}