package org.example.bank.service;

import org.example.bank.entity.Card;
import org.example.bank.entity.User;
import org.example.bank.entity.enums.CardStatus;
import org.example.bank.entity.enums.Role;
import org.example.bank.repository.CardRepository;
import org.example.bank.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class CardExpirationSchedulerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private CardExpirationScheduler cardExpirationScheduler;

    @Autowired
    private CardRepository cardRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;

    @BeforeEach
    void setUp() {
        cardRepository.deleteAll();
        userRepository.deleteAll();

        testUser = User.builder()
                .username("scheduleruser")
                .password(passwordEncoder.encode("pass"))
                .email("scheduler@example.com")
                .fullName("Scheduler User")
                .role(Role.ROLE_USER)
                .isActive(true)
                .build();
        userRepository.save(testUser);
    }

    @Test
    void updateExpiredCards_ShouldUpdateExpiredCardsInDatabase() {
        // Given
        Card activeNotExpired = Card.builder()
                .encryptedCardNumber("encrypted123")
                .maskedNumber("**** **** **** 1111")
                .owner(testUser)
                .expiryDate(LocalDate.now().plusYears(2))
                .status(CardStatus.ACTIVE)
                .build();

        Card activeExpired = Card.builder()
                .encryptedCardNumber("encrypted456")
                .maskedNumber("**** **** **** 2222")
                .owner(testUser)
                .expiryDate(LocalDate.now().minusDays(1))
                .status(CardStatus.ACTIVE)
                .build();

        Card blockedExpired = Card.builder()
                .encryptedCardNumber("encrypted789")
                .maskedNumber("**** **** **** 3333")
                .owner(testUser)
                .expiryDate(LocalDate.now().minusMonths(1))
                .status(CardStatus.BLOCKED)
                .build();

        cardRepository.saveAll(List.of(activeNotExpired, activeExpired, blockedExpired));

        // When
        cardExpirationScheduler.updateExpiredCards();

        // Then
        Card updatedActiveExpired = cardRepository.findById(activeExpired.getId()).orElseThrow();
        Card updatedBlockedExpired = cardRepository.findById(blockedExpired.getId()).orElseThrow();
        Card updatedActiveNotExpired = cardRepository.findById(activeNotExpired.getId()).orElseThrow();

        assertThat(updatedActiveExpired.getStatus()).isEqualTo(CardStatus.EXPIRED);
        assertThat(updatedBlockedExpired.getStatus()).isEqualTo(CardStatus.BLOCKED); // Не изменился
        assertThat(updatedActiveNotExpired.getStatus()).isEqualTo(CardStatus.ACTIVE); // Не изменился
    }

    @Test
    void updateExpiredCards_ShouldHandleMultipleExpiredCardsInDatabase() {
        // Given
        Card expiredCard1 = Card.builder()
                .encryptedCardNumber("encrypted111")
                .maskedNumber("**** **** **** 1111")
                .owner(testUser)
                .expiryDate(LocalDate.now().minusDays(5))
                .status(CardStatus.ACTIVE)
                .build();

        Card expiredCard2 = Card.builder()
                .encryptedCardNumber("encrypted222")
                .maskedNumber("**** **** **** 2222")
                .owner(testUser)
                .expiryDate(LocalDate.now().minusMonths(2))
                .status(CardStatus.ACTIVE)
                .build();

        Card notExpiredCard = Card.builder()
                .encryptedCardNumber("encrypted333")
                .maskedNumber("**** **** **** 3333")
                .owner(testUser)
                .expiryDate(LocalDate.now().plusMonths(6))
                .status(CardStatus.ACTIVE)
                .build();

        cardRepository.saveAll(List.of(expiredCard1, expiredCard2, notExpiredCard));

        // When
        cardExpirationScheduler.updateExpiredCards();

        // Then
        List<Card> allCards = cardRepository.findAll();
        long expiredCount = allCards.stream()
                .filter(card -> card.getStatus() == CardStatus.EXPIRED)
                .count();

        assertThat(expiredCount).isEqualTo(2);
        assertThat(notExpiredCard.getStatus()).isEqualTo(CardStatus.ACTIVE);
    }

    @Test
    void updateExpiredCards_ShouldBeIdempotent() {
        // Given
        Card activeExpired = Card.builder()
                .encryptedCardNumber("encrypted456")
                .maskedNumber("**** **** **** 2222")
                .owner(testUser)
                .expiryDate(LocalDate.now().minusDays(1))
                .status(CardStatus.ACTIVE)
                .build();

        cardRepository.save(activeExpired);

        // When
        cardExpirationScheduler.updateExpiredCards();
        cardExpirationScheduler.updateExpiredCards();

        // Then
        Card updatedCard = cardRepository.findById(activeExpired.getId()).orElseThrow();
        assertThat(updatedCard.getStatus()).isEqualTo(CardStatus.EXPIRED);

        // Проверяем что статус не менялся повторно
        assertThat(updatedCard.getStatus()).isEqualTo(CardStatus.EXPIRED);
    }
}