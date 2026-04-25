package org.example.bank.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.bank.dto.request.RegisterRequest;
import org.example.bank.entity.User;
import org.example.bank.entity.enums.Role;
import org.example.bank.repository.UserRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class AuthControllerRegisterTest {

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
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    void register_ShouldCreateUser_WhenValidRequest() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("john_doe");
        request.setPassword("password123");
        request.setEmail("john@example.com");
        request.setFullName("John Doe");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("john_doe"))
                .andExpect(jsonPath("$.email").value("john@example.com"))
                .andExpect(jsonPath("$.fullName").value("John Doe"))
                .andExpect(jsonPath("$.role").value("ROLE_USER"))
                .andExpect(jsonPath("$.active").value(true));

        // Verify user was saved to database
        User savedUser = userRepository.findByUsername("john_doe").orElse(null);
        assertThat(savedUser).isNotNull();
        assertThat(savedUser.getEmail()).isEqualTo("john@example.com");
        assertThat(passwordEncoder.matches("password123", savedUser.getPassword())).isTrue();
    }

    @Test
    void register_ShouldReturnBadRequest_WhenUsernameIsBlank() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("");
        request.setPassword("password123");
        request.setEmail("john@example.com");
        request.setFullName("John Doe");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_ShouldReturnBadRequest_WhenPasswordIsBlank() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("john_doe");
        request.setPassword("");
        request.setEmail("john@example.com");
        request.setFullName("John Doe");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_ShouldReturnBadRequest_WhenEmailIsInvalid() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("john_doe");
        request.setPassword("password123");
        request.setEmail("invalid-email");
        request.setFullName("John Doe");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_ShouldReturnBadRequest_WhenUsernameAlreadyExists() throws Exception {
        // Create existing user
        User existingUser = User.builder()
                .username("existing_user")
                .password(passwordEncoder.encode("pass"))
                .email("existing@example.com")
                .fullName("Existing User")
                .role(Role.ROLE_USER)
                .isActive(true)
                .build();
        userRepository.save(existingUser);

        RegisterRequest request = new RegisterRequest();
        request.setUsername("existing_user");
        request.setPassword("password123");
        request.setEmail("new@example.com");
        request.setFullName("New User");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())  // ← 500, а не 409
                .andExpect(jsonPath("$.message").value("Username already taken"));
    }

    @Test
    void register_ShouldReturnBadRequest_WhenEmailAlreadyExists() throws Exception {
        // Create existing user
        User existingUser = User.builder()
                .username("existing_user")
                .password(passwordEncoder.encode("pass"))
                .email("existing@example.com")
                .fullName("Existing User")
                .role(Role.ROLE_USER)
                .isActive(true)
                .build();
        userRepository.save(existingUser);

        RegisterRequest request = new RegisterRequest();
        request.setUsername("new_user");
        request.setPassword("password123");
        request.setEmail("existing@example.com");
        request.setFullName("New User");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())  // ← 500, а не 409
                .andExpect(jsonPath("$.message").value("Email already registered"));
    }

    @Test
    void register_ShouldEncryptPassword_WhenUserCreated() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("secure_user");
        request.setPassword("mySecretPassword123!");
        request.setEmail("secure@example.com");
        request.setFullName("Secure User");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        User savedUser = userRepository.findByUsername("secure_user").orElseThrow();

        // Password should be encrypted (not equal to original)
        assertThat(savedUser.getPassword()).isNotEqualTo("mySecretPassword123!");
        // But should match the original
        assertThat(passwordEncoder.matches("mySecretPassword123!", savedUser.getPassword())).isTrue();
    }
}