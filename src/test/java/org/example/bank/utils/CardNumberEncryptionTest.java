package org.example.bank.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;

import static org.assertj.core.api.Assertions.assertThat;

class CardNumberEncryptionTest {

    private TextEncryptor textEncryptor;
    private CardNumberEncryptor cardNumberEncryptor;

    @BeforeEach
    void setUp() {
        String password = "testPassword2024";
        String salt = "12345678";
        textEncryptor = Encryptors.text(password, salt);
        cardNumberEncryptor = new CardNumberEncryptor(textEncryptor);
    }

    @Test
    void encrypt_ShouldReturnDifferentValue_WhenCardNumberProvided() {
        String cardNumber = "1234567890123456";

        String encrypted = cardNumberEncryptor.encrypt(cardNumber);

        assertThat(encrypted).isNotEqualTo(cardNumber);
        assertThat(encrypted).isNotBlank();
    }

    @Test
    void decrypt_ShouldReturnOriginalValue_WhenEncrypted() {
        String originalCardNumber = "1234567890123456";

        String encrypted = cardNumberEncryptor.encrypt(originalCardNumber);
        String decrypted = cardNumberEncryptor.decrypt(encrypted);

        assertThat(decrypted).isEqualTo(originalCardNumber);
    }

    @Test
    void encrypt_ShouldReturnDifferentValues_ForSameInput() {
        String cardNumber = "1234567890123456";

        String encrypted1 = cardNumberEncryptor.encrypt(cardNumber);
        String encrypted2 = cardNumberEncryptor.encrypt(cardNumber);

        assertThat(encrypted1).isNotEqualTo(encrypted2);
    }

    @Test
    void decrypt_ShouldThrowException_WhenInvalidData() {
        org.junit.jupiter.api.Assertions.assertThrows(Exception.class, () -> {
            cardNumberEncryptor.decrypt("invalid_encrypted_data");
        });
    }
}