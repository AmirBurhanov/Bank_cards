package org.example.bank.utils;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class CardNumberGenerator {

    private static final SecureRandom random = new SecureRandom();

    public String generateEncryptedNumber() {
        // Генерируем 16-значный номер карты
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 16; i++) {
            sb.append(random.nextInt(10));
        }
        // В реальном проекте здесь нужно шифрование
        // Пока возвращаем как есть, шифрование добавим позже
        return sb.toString();
    }
}