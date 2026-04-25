package org.example.bank.controller;

import org.example.bank.entity.User;
import org.example.bank.entity.enums.Role;
import org.example.bank.repository.CardRepository;
import org.example.bank.repository.UserRepository;
import org.example.bank.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class AdminControllerIntegrationTest {

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
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CardRepository cardRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private String adminToken;
    private String userToken;

    @BeforeEach
    void setUp() {
        cardRepository.deleteAll();
        userRepository.deleteAll();

        // Create admin user
        User adminUser = User.builder()
                .username("admin")
                .password(passwordEncoder.encode("admin123"))
                .email("admin@example.com")
                .fullName("Admin User")
                .role(Role.ROLE_ADMIN)
                .isActive(true)
                .build();
        userRepository.save(adminUser);

        // Create regular user
        User regularUser = User.builder()
                .username("user")
                .password(passwordEncoder.encode("user123"))
                .email("user@example.com")
                .fullName("Regular User")
                .role(Role.ROLE_USER)
                .isActive(true)
                .build();
        userRepository.save(regularUser);

        adminToken = jwtTokenProvider.generateTokenFromUsername("admin");
        userToken = jwtTokenProvider.generateTokenFromUsername("user");
    }

    @Test
    void getAllCards_ShouldReturnAllCards_WhenAdmin() throws Exception {
        mockMvc.perform(get("/api/admin/cards")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void getAllCards_ShouldReturnForbidden_WhenUser() throws Exception {
        mockMvc.perform(get("/api/admin/cards")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAllUsers_ShouldReturnAllUsers_WhenAdmin() throws Exception {
        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2));
    }

    @Test
    void deleteUser_ShouldSoftDeleteUser_WhenAdmin() throws Exception {
        // Get user ID
        User user = userRepository.findByUsername("user").orElseThrow();

        mockMvc.perform(delete("/api/admin/users/{userId}", user.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        // Verify user is deactivated
        User deletedUser = userRepository.findById(user.getId()).orElseThrow();
        assert !deletedUser.isActive();
    }
}