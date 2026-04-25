package org.example.bank.repository;

import org.example.bank.BankCardsApplication;
import org.example.bank.entity.User;
import org.example.bank.entity.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = BankCardsApplication.class)
class UserRepositoryTest {

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
        // Отключаем Liquibase для тестов
        registry.add("spring.liquibase.enabled", () -> false);
        // Отключаем автоматическое создание схемы
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @Autowired
    private UserRepository userRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .username("testuser")
                .password("encodedpass")
                .email("test@example.com")
                .fullName("Test User")
                .role(Role.ROLE_USER)
                .isActive(true)
                .build();
        userRepository.save(testUser);
    }

    @Test
    void findByUsername_ShouldReturnUser_WhenExists() {
        var found = userRepository.findByUsername("testuser");

        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("test@example.com");
    }

    @Test
    void findByUsername_ShouldReturnEmpty_WhenNotExists() {
        var found = userRepository.findByUsername("nonexistent");

        assertThat(found).isEmpty();
    }

    @Test
    void existsByUsername_ShouldReturnTrue_WhenExists() {
        boolean exists = userRepository.existsByUsername("testuser");

        assertThat(exists).isTrue();
    }

    @Test
    void existsByEmail_ShouldReturnTrue_WhenExists() {
        boolean exists = userRepository.existsByEmail("test@example.com");

        assertThat(exists).isTrue();
    }
}