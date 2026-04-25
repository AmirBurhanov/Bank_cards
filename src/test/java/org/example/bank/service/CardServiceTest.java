package org.example.bank.service;

import org.example.bank.dto.request.CardRequest;
import org.example.bank.dto.response.CardResponse;
import org.example.bank.entity.Card;
import org.example.bank.entity.User;
import org.example.bank.entity.enums.CardStatus;
import org.example.bank.entity.enums.Role;
import org.example.bank.exception.CardNotFoundException;
import org.example.bank.exception.InsufficientFundsException;
import org.example.bank.exception.UnauthorizedAccessException;
import org.example.bank.repository.CardRepository;
import org.example.bank.repository.UserRepository;
import org.example.bank.utils.CardNumberGenerator;
import org.example.bank.utils.CardNumberMasker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CardServiceTest {

    @Mock
    private CardRepository cardRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CardNumberGenerator cardNumberGenerator;

    @Mock
    private CardNumberMasker cardNumberMasker;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private CardService cardService;

    private User testUser;
    private Card testCard;
    private Card testCard2;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .fullName("Test User")
                .role(Role.ROLE_USER)
                .isActive(true)
                .build();

        testCard = Card.builder()
                .id(1L)
                .encryptedCardNumber("1234567890123456")
                .maskedNumber("**** **** **** 3456")
                .owner(testUser)
                .expiryDate(LocalDate.now().plusYears(3))
                .status(CardStatus.ACTIVE)
                .balance(BigDecimal.valueOf(1000))
                .build();

        testCard2 = Card.builder()
                .id(2L)
                .encryptedCardNumber("6543210987654321")
                .maskedNumber("**** **** **** 4321")
                .owner(testUser)
                .expiryDate(LocalDate.now().plusYears(2))
                .status(CardStatus.ACTIVE)
                .balance(BigDecimal.valueOf(500))
                .build();
    }

    private void setupSecurityContext() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
        when(authentication.getName()).thenReturn("testuser");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
    }
    @Test
    void createCard_ShouldCreateNewCard_WhenValidRequest() {
        setupSecurityContext();
        CardRequest request = new CardRequest();
        request.setInitialBalance(BigDecimal.valueOf(100));

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(cardNumberGenerator.generateEncryptedNumber()).thenReturn("1111222233334444");
        when(cardNumberMasker.mask(anyString())).thenReturn("**** **** **** 4444");
        when(cardRepository.save(any(Card.class))).thenReturn(testCard);

        CardResponse response = cardService.createCard(request);

        assertThat(response).isNotNull();
        assertThat(response.getOwnerName()).isEqualTo("Test User");
        assertThat(response.getBalance()).isEqualTo(BigDecimal.valueOf(1000));

        verify(cardRepository, times(1)).save(any(Card.class));
    }

    @Test
    void createCard_ShouldCreateCardWithZeroBalance_WhenNoInitialBalance() {
        setupSecurityContext();
        CardRequest request = new CardRequest();

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(cardNumberGenerator.generateEncryptedNumber()).thenReturn("1111222233334444");
        when(cardNumberMasker.mask(anyString())).thenReturn("**** **** **** 4444");
        when(cardRepository.save(any(Card.class))).thenReturn(testCard);

        CardResponse response = cardService.createCard(request);

        assertThat(response).isNotNull();
        verify(cardRepository, times(1)).save(any(Card.class));
    }

    @Test
    void getMyCards_ShouldReturnUserCards_WhenNoFilters() {
        setupSecurityContext();
        Pageable pageable = PageRequest.of(0, 10);
        Page<Card> cardPage = new PageImpl<>(List.of(testCard, testCard2));

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(cardRepository.findByOwner(testUser, pageable)).thenReturn(cardPage);

        Page<CardResponse> result = cardService.getMyCards(null, null, pageable);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getMaskedNumber()).isEqualTo("**** **** **** 3456");
        verify(cardRepository, times(1)).findByOwner(testUser, pageable);
    }

    @Test
    void getMyCards_ShouldReturnFilteredByStatus_WhenStatusProvided() {
        setupSecurityContext();
        Pageable pageable = PageRequest.of(0, 10);
        Page<Card> cardPage = new PageImpl<>(List.of(testCard));

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(cardRepository.findByOwnerAndStatus(testUser, CardStatus.ACTIVE, pageable))
                .thenReturn(cardPage);

        Page<CardResponse> result = cardService.getMyCards(null, CardStatus.ACTIVE, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getStatus()).isEqualTo(CardStatus.ACTIVE);
        verify(cardRepository, times(1)).findByOwnerAndStatus(testUser, CardStatus.ACTIVE, pageable);
    }

    @Test
    void getMyCards_ShouldReturnSearchResults_WhenSearchProvided() {
        setupSecurityContext();
        Pageable pageable = PageRequest.of(0, 10);
        Page<Card> cardPage = new PageImpl<>(List.of(testCard));
        String search = "3456";

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(cardRepository.searchByOwnerAndMaskedNumber(testUser, search, pageable))
                .thenReturn(cardPage);

        Page<CardResponse> result = cardService.getMyCards(search, null, pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(cardRepository, times(1)).searchByOwnerAndMaskedNumber(testUser, search, pageable);
    }

    @Test
    void getBalance_ShouldReturnBalance_WhenCardOwnedByUser() {
        setupSecurityContext();
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(cardRepository.findById(1L)).thenReturn(Optional.of(testCard));

        BigDecimal balance = cardService.getBalance(1L);

        assertThat(balance).isEqualByComparingTo(BigDecimal.valueOf(1000));
    }

    @Test
    void getBalance_ShouldThrowException_WhenCardNotFound() {
        setupSecurityContext();
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(cardRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cardService.getBalance(999L))
                .isInstanceOf(CardNotFoundException.class)
                .hasMessageContaining("Card not found with id: 999");
    }

    @Test
    void getBalance_ShouldThrowException_WhenCardNotOwnedByUser() {
        setupSecurityContext();
        User otherUser = User.builder().id(999L).username("other").build();
        Card otherCard = Card.builder().id(3L).owner(otherUser).build();

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(cardRepository.findById(3L)).thenReturn(Optional.of(otherCard));

        assertThatThrownBy(() -> cardService.getBalance(3L))
                .isInstanceOf(UnauthorizedAccessException.class)
                .hasMessageContaining("You don't own this card");
    }

    @Test
    void blockCard_ShouldBlockCard_WhenCardIsActive() {
        setupSecurityContext();
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(cardRepository.findById(1L)).thenReturn(Optional.of(testCard));

        cardService.blockCard(1L);

        assertThat(testCard.getStatus()).isEqualTo(CardStatus.BLOCKED);
        verify(cardRepository, times(1)).save(testCard);
    }

    @Test
    void blockCard_ShouldThrowException_WhenCardAlreadyBlocked() {
        setupSecurityContext();
        testCard.setStatus(CardStatus.BLOCKED);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(cardRepository.findById(1L)).thenReturn(Optional.of(testCard));

        assertThatThrownBy(() -> cardService.blockCard(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Card is already blocked");
    }

    @Test
    void transferBetweenCards_ShouldTransferAmount_WhenValid() {
        setupSecurityContext();
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(cardRepository.findById(1L)).thenReturn(Optional.of(testCard));
        when(cardRepository.findById(2L)).thenReturn(Optional.of(testCard2));

        BigDecimal amount = BigDecimal.valueOf(200);

        cardService.transferBetweenCards(1L, 2L, amount);

        assertThat(testCard.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(800));
        assertThat(testCard2.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(700));
        verify(cardRepository, times(1)).save(testCard);
        verify(cardRepository, times(1)).save(testCard2);
    }

    @Test
    void transferBetweenCards_ShouldThrowException_WhenInsufficientFunds() {
        setupSecurityContext();
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(cardRepository.findById(1L)).thenReturn(Optional.of(testCard));
        when(cardRepository.findById(2L)).thenReturn(Optional.of(testCard2));

        BigDecimal amount = BigDecimal.valueOf(2000);

        assertThatThrownBy(() -> cardService.transferBetweenCards(1L, 2L, amount))
                .isInstanceOf(InsufficientFundsException.class)
                .hasMessageContaining("Insufficient funds");
    }
    @Test
    void transferBetweenCards_ShouldThrowException_WhenAmountIsZeroOrNegative() {
        BigDecimal amount = BigDecimal.ZERO;

        assertThatThrownBy(() -> cardService.transferBetweenCards(1L, 2L, amount))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Transfer amount must be positive");

        verify(userRepository, never()).findByUsername(anyString());
        verify(cardRepository, never()).findById(anyLong());
    }

    @Test
    void transferBetweenCards_ShouldThrowException_WhenTransferringToSameCard() {
        BigDecimal amount = BigDecimal.valueOf(100);

        assertThatThrownBy(() -> cardService.transferBetweenCards(1L, 1L, amount))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot transfer to the same card");

        verify(userRepository, never()).findByUsername(anyString());
        verify(cardRepository, never()).findById(anyLong());
    }

    @Test
    void transferBetweenCards_ShouldThrowException_WhenSourceCardIsBlocked() {
        setupSecurityContext();
        testCard.setStatus(CardStatus.BLOCKED);

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(cardRepository.findById(1L)).thenReturn(Optional.of(testCard));
        when(cardRepository.findById(2L)).thenReturn(Optional.of(testCard2));

        BigDecimal amount = BigDecimal.valueOf(100);

        assertThatThrownBy(() -> cardService.transferBetweenCards(1L, 2L, amount))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Source card is not active");
    }

    @Test
    void activateCard_ShouldActivateCard_WhenCardIsBlocked() {
        setupSecurityContext();
        testCard.setStatus(CardStatus.BLOCKED);

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(cardRepository.findById(1L)).thenReturn(Optional.of(testCard));

        cardService.activateCard(1L);

        assertThat(testCard.getStatus()).isEqualTo(CardStatus.ACTIVE);
        verify(cardRepository, times(1)).save(testCard);
    }

    @Test
    void activateCard_ShouldThrowException_WhenCardIsExpired() {
        setupSecurityContext();
        testCard.setStatus(CardStatus.BLOCKED);
        testCard.setExpiryDate(LocalDate.now().minusDays(1));

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(cardRepository.findById(1L)).thenReturn(Optional.of(testCard));

        assertThatThrownBy(() -> cardService.activateCard(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot activate expired card");
    }
}