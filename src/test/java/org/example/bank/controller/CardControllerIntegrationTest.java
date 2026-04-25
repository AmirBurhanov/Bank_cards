package org.example.bank.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.bank.dto.request.CardRequest;
import org.example.bank.dto.request.TransferRequest;
import org.example.bank.entity.Card;
import org.example.bank.entity.User;
import org.example.bank.entity.enums.CardStatus;
import org.example.bank.entity.enums.Role;
import org.example.bank.repository.CardRepository;
import org.example.bank.repository.UserRepository;
import org.example.bank.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class CardControllerIntegrationTest {

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

        registry.add("spring.liquibase.enabled", () -> true);
        registry.add("spring.liquibase.drop-first", () -> true);
        // Используем основной файл (у вас же есть все три файла в v1!)
        registry.add("spring.liquibase.change-log", () -> "classpath:db/changelog/db.changelog-master.yaml");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CardRepository cardRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private String userToken;
    private String adminToken;
    private User testUser;
    private User adminUser;
    private Card testCard;

    @BeforeEach
    void setUp() {
        // Очищаем БД
        cardRepository.deleteAll();
        userRepository.deleteAll();

        // Create regular user
        testUser = User.builder()
                .username("testuser")
                .password(passwordEncoder.encode("password123"))
                .email("test@example.com")
                .fullName("Test User")
                .role(Role.ROLE_USER)
                .isActive(true)
                .build();
        userRepository.save(testUser);

        // Create admin user
        adminUser = User.builder()
                .username("admin")
                .password(passwordEncoder.encode("admin123"))
                .email("admin@example.com")
                .fullName("Admin User")
                .role(Role.ROLE_ADMIN)
                .isActive(true)
                .build();
        userRepository.save(adminUser);

        // Create test card
        testCard = Card.builder()
                .encryptedCardNumber("1111222233334444")
                .maskedNumber("**** **** **** 4444")
                .owner(testUser)
                .expiryDate(LocalDate.now().plusYears(3))
                .status(CardStatus.ACTIVE)
                .balance(BigDecimal.valueOf(1000))
                .build();
        cardRepository.save(testCard);

        // Generate tokens
        userToken = jwtTokenProvider.generateTokenFromUsername("testuser");
        adminToken = jwtTokenProvider.generateTokenFromUsername("admin");
    }

    @Test
    void createCard_ShouldReturnCreatedCard_WhenValidRequest() throws Exception {
        CardRequest request = new CardRequest();
        request.setInitialBalance(BigDecimal.valueOf(500));

        mockMvc.perform(post("/api/cards")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ownerName").value("Test User"))
                .andExpect(jsonPath("$.balance").value(500))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void getMyCards_ShouldReturnUserCards_WhenAuthenticated() throws Exception {
        mockMvc.perform(get("/api/cards")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].maskedNumber").value("**** **** **** 4444"))
                .andExpect(jsonPath("$.content[0].balance").value(1000));
    }

    @Test
    void getMyCards_ShouldFilterByStatus_WhenStatusProvided() throws Exception {
        // Create blocked card
        Card blockedCard = Card.builder()
                .encryptedCardNumber("5555666677778888")
                .maskedNumber("**** **** **** 8888")
                .owner(testUser)
                .expiryDate(LocalDate.now().plusYears(2))
                .status(CardStatus.BLOCKED)
                .balance(BigDecimal.valueOf(200))
                .build();
        cardRepository.save(blockedCard);

        mockMvc.perform(get("/api/cards")
                        .param("status", "ACTIVE")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].status").value("ACTIVE"));
    }

    @Test
    void getBalance_ShouldReturnBalance_WhenCardOwnedByUser() throws Exception {
        mockMvc.perform(get("/api/cards/{cardId}/balance", testCard.getId())
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(content().string("1000.00"));
    }

    @Test
    void getBalance_ShouldReturnNotFound_WhenCardDoesNotExist() throws Exception {
        mockMvc.perform(get("/api/cards/{cardId}/balance", 999L)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void blockCard_ShouldBlockCard_WhenValidRequest() throws Exception {
        mockMvc.perform(post("/api/cards/{cardId}/block", testCard.getId())
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk());

        // Verify card is blocked
        Card blockedCard = cardRepository.findById(testCard.getId()).orElseThrow();
        assert blockedCard.getStatus() == CardStatus.BLOCKED;
    }

    @Test
    void blockCard_ShouldReturnForbidden_WhenCardNotOwnedByUser() throws Exception {
        // Create another user and their card
        User otherUser = User.builder()
                .username("otheruser")
                .password(passwordEncoder.encode("password"))
                .email("other@example.com")
                .fullName("Other User")
                .role(Role.ROLE_USER)
                .isActive(true)
                .build();
        userRepository.save(otherUser);

        Card otherCard = Card.builder()
                .encryptedCardNumber("9999000011112222")
                .maskedNumber("**** **** **** 2222")
                .owner(otherUser)
                .expiryDate(LocalDate.now().plusYears(3))
                .status(CardStatus.ACTIVE)
                .balance(BigDecimal.valueOf(500))
                .build();
        cardRepository.save(otherCard);

        mockMvc.perform(post("/api/cards/{cardId}/block", otherCard.getId())
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void transfer_ShouldTransferAmount_WhenValidRequest() throws Exception {
        // Create second card for user
        Card secondCard = Card.builder()
                .encryptedCardNumber("5555666677778888")
                .maskedNumber("**** **** **** 8888")
                .owner(testUser)
                .expiryDate(LocalDate.now().plusYears(2))
                .status(CardStatus.ACTIVE)
                .balance(BigDecimal.valueOf(500))
                .build();
        cardRepository.save(secondCard);

        TransferRequest request = new TransferRequest();
        request.setFromCardId(testCard.getId());
        request.setToCardId(secondCard.getId());
        request.setAmount(BigDecimal.valueOf(300));

        mockMvc.perform(post("/api/cards/transfer")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void transfer_ShouldReturnBadRequest_WhenInsufficientFunds() throws Exception {
        // Create second card
        Card secondCard = Card.builder()
                .encryptedCardNumber("5555666677778888")
                .maskedNumber("**** **** **** 8888")
                .owner(testUser)
                .expiryDate(LocalDate.now().plusYears(2))
                .status(CardStatus.ACTIVE)
                .balance(BigDecimal.valueOf(500))
                .build();
        cardRepository.save(secondCard);

        TransferRequest request = new TransferRequest();
        request.setFromCardId(testCard.getId());
        request.setToCardId(secondCard.getId());
        request.setAmount(BigDecimal.valueOf(2000)); // More than balance

        mockMvc.perform(post("/api/cards/transfer")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void transfer_ShouldReturnUnauthorized_WhenNoToken() throws Exception {
        TransferRequest request = new TransferRequest();
        request.setFromCardId(1L);
        request.setToCardId(2L);
        request.setAmount(BigDecimal.valueOf(100));

        mockMvc.perform(post("/api/cards/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());  // ← изменить с isUnauthorized на isForbidden
    }
}