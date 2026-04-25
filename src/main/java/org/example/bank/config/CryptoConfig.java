package org.example.bank.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;

@Configuration
public class CryptoConfig {

    @Value("${crypto.password}")
    private String password;

    @Value("${crypto.salt}")
    private String salt;

    @Bean
    public TextEncryptor textEncryptor() {
        return Encryptors.text(password, salt);
    }
}