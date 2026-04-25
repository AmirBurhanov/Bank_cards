package org.example.bank.repository;

import org.example.bank.BankCardsApplication;
import org.example.bank.entity.Card;
import org.example.bank.entity.User;
import org.example.bank.entity.enums.CardStatus;
import org.example.bank.entity.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = BankCardsApplication.class)
class CardRepositoryTest {

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
        registry.add("spring.liquibase.enabled", () -> false);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @Autowired
    private CardRepository cardRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser;
    private Card testCard1;
    private Card testCard2;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .username("carduser")
                .password("encodedpass")
                .email("card@example.com")
                .fullName("Card User")
                .role(Role.ROLE_USER)
                .isActive(true)
                .build();
        userRepository.save(testUser);

        testCard1 = Card.builder()
                .encryptedCardNumber("encrypted123")
                .maskedNumber("**** **** **** 1111")
                .owner(testUser)
                .expiryDate(LocalDate.now().plusYears(2))
                .status(CardStatus.ACTIVE)
                .balance(BigDecimal.valueOf(1000))
                .build();

        testCard2 = Card.builder()
                .encryptedCardNumber("encrypted456")
                .maskedNumber("**** **** **** 2222")
                .owner(testUser)
                .expiryDate(LocalDate.now().minusYears(1))
                .status(CardStatus.EXPIRED)
                .balance(BigDecimal.valueOf(500))
                .build();

        cardRepository.save(testCard1);
        cardRepository.save(testCard2);
    }

    @Test
    void findByOwner_ShouldReturnCards_WhenExists() {
        Pageable pageable = PageRequest.of(0, 10);
        var cards = cardRepository.findByOwner(testUser, pageable);

        assertThat(cards.getContent()).hasSize(2);
        assertThat(cards.getContent()).extracting("maskedNumber")
                .contains("**** **** **** 1111", "**** **** **** 2222");
    }

    @Test
    void findByOwnerAndStatus_ShouldReturnFilteredCards() {
        Pageable pageable = PageRequest.of(0, 10);
        var activeCards = cardRepository.findByOwnerAndStatus(testUser, CardStatus.ACTIVE, pageable);

        assertThat(activeCards.getContent()).hasSize(1);
        assertThat(activeCards.getContent().get(0).getStatus()).isEqualTo(CardStatus.ACTIVE);
    }

    @Test
    void findByIdAndOwner_ShouldReturnCard_WhenMatches() {
        var found = cardRepository.findByIdAndOwner(testCard1.getId(), testUser);

        assertThat(found).isPresent();
        assertThat(found.get().getMaskedNumber()).isEqualTo("**** **** **** 1111");
    }

    @Test
    void existsByIdAndOwner_ShouldReturnTrue_WhenMatches() {
        boolean exists = cardRepository.existsByIdAndOwner(testCard1.getId(), testUser);

        assertThat(exists).isTrue();
    }

    @Test
    void findByOwner_ShouldReturnEmpty_WhenNoCards() {
        User newUser = User.builder()
                .username("emptyuser")
                .password("pass")
                .email("empty@example.com")
                .fullName("Empty User")
                .role(Role.ROLE_USER)
                .isActive(true)
                .build();
        userRepository.save(newUser);

        Pageable pageable = PageRequest.of(0, 10);
        var cards = cardRepository.findByOwner(newUser, pageable);

        assertThat(cards.getContent()).isEmpty();
    }

    @Test
    void searchByOwnerAndMaskedNumber_ShouldReturnMatchingCards() {
        Pageable pageable = PageRequest.of(0, 10);
        var found = cardRepository.searchByOwnerAndMaskedNumber(testUser, "1111", pageable);

        assertThat(found.getContent()).hasSize(1);
        assertThat(found.getContent().get(0).getMaskedNumber()).contains("1111");
    }
}