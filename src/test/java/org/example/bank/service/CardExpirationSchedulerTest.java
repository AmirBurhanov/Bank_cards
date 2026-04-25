package org.example.bank.service;

import org.example.bank.entity.Card;
import org.example.bank.entity.User;
import org.example.bank.entity.enums.CardStatus;
import org.example.bank.entity.enums.Role;
import org.example.bank.repository.CardRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CardExpirationSchedulerTest {

    @Mock
    private CardRepository cardRepository;

    @InjectMocks
    private CardExpirationScheduler cardExpirationScheduler;

    private User testUser;
    private Card activeCardNotExpired;
    private Card activeCardExpired;
    private Card blockedCardExpired;
    private Card expiredCardAlreadyExpired;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .role(Role.ROLE_USER)
                .build();

        activeCardNotExpired = Card.builder()
                .id(1L)
                .owner(testUser)
                .status(CardStatus.ACTIVE)
                .expiryDate(LocalDate.now().plusYears(2))
                .build();

        activeCardExpired = Card.builder()
                .id(2L)
                .owner(testUser)
                .status(CardStatus.ACTIVE)
                .expiryDate(LocalDate.now().minusDays(1))
                .build();

        blockedCardExpired = Card.builder()
                .id(3L)
                .owner(testUser)
                .status(CardStatus.BLOCKED)
                .expiryDate(LocalDate.now().minusMonths(1))
                .build();

        expiredCardAlreadyExpired = Card.builder()
                .id(4L)
                .owner(testUser)
                .status(CardStatus.EXPIRED)
                .expiryDate(LocalDate.now().minusDays(5))
                .build();
    }

    @Test
    void updateExpiredCards_ShouldUpdateOnlyActiveAndExpiredCards() {
        List<Card> allCards = List.of(
                activeCardNotExpired,
                activeCardExpired,
                blockedCardExpired,
                expiredCardAlreadyExpired
        );

        when(cardRepository.findAll()).thenReturn(allCards);
        when(cardRepository.save(any(Card.class))).thenAnswer(invocation -> invocation.getArgument(0));

        cardExpirationScheduler.updateExpiredCards();

        assertThat(activeCardNotExpired.getStatus()).isEqualTo(CardStatus.ACTIVE);
        assertThat(activeCardExpired.getStatus()).isEqualTo(CardStatus.EXPIRED);
        assertThat(blockedCardExpired.getStatus()).isEqualTo(CardStatus.BLOCKED);
        assertThat(expiredCardAlreadyExpired.getStatus()).isEqualTo(CardStatus.EXPIRED);

        verify(cardRepository, times(1)).save(activeCardExpired);
        verify(cardRepository, never()).save(activeCardNotExpired);
        verify(cardRepository, never()).save(blockedCardExpired);
        verify(cardRepository, never()).save(expiredCardAlreadyExpired);
    }

    @Test
    void updateExpiredCards_ShouldHandleMultipleExpiredCards() {
        Card expiredCard1 = Card.builder()
                .id(5L)
                .owner(testUser)
                .status(CardStatus.ACTIVE)
                .expiryDate(LocalDate.now().minusDays(10))
                .build();

        Card expiredCard2 = Card.builder()
                .id(6L)
                .owner(testUser)
                .status(CardStatus.ACTIVE)
                .expiryDate(LocalDate.now().minusMonths(2))
                .build();

        Card expiredCard3 = Card.builder()
                .id(7L)
                .owner(testUser)
                .status(CardStatus.ACTIVE)
                .expiryDate(LocalDate.now().minusYears(1))
                .build();

        List<Card> allCards = List.of(expiredCard1, expiredCard2, expiredCard3);
        when(cardRepository.findAll()).thenReturn(allCards);
        when(cardRepository.save(any(Card.class))).thenAnswer(invocation -> invocation.getArgument(0));

        cardExpirationScheduler.updateExpiredCards();

        assertThat(expiredCard1.getStatus()).isEqualTo(CardStatus.EXPIRED);
        assertThat(expiredCard2.getStatus()).isEqualTo(CardStatus.EXPIRED);
        assertThat(expiredCard3.getStatus()).isEqualTo(CardStatus.EXPIRED);

        verify(cardRepository, times(3)).save(any(Card.class));
    }

    @Test
    void updateExpiredCards_ShouldDoNothing_WhenNoExpiredCards() {
        List<Card> allCards = List.of(activeCardNotExpired);
        when(cardRepository.findAll()).thenReturn(allCards);

        cardExpirationScheduler.updateExpiredCards();

        assertThat(activeCardNotExpired.getStatus()).isEqualTo(CardStatus.ACTIVE);
        verify(cardRepository, never()).save(any(Card.class));
    }

    @Test
    void updateExpiredCards_ShouldDoNothing_WhenNoCards() {
        when(cardRepository.findAll()).thenReturn(List.of());

        cardExpirationScheduler.updateExpiredCards();

        verify(cardRepository, never()).save(any(Card.class));
    }

    @Test
    void updateExpiredCards_ShouldNotUpdateCardsExpiringToday() {
        Card cardExpiringToday = Card.builder()
                .id(8L)
                .owner(testUser)
                .status(CardStatus.ACTIVE)
                .expiryDate(LocalDate.now())
                .build();

        List<Card> allCards = List.of(cardExpiringToday);
        when(cardRepository.findAll()).thenReturn(allCards);

        cardExpirationScheduler.updateExpiredCards();

        assertThat(cardExpiringToday.getStatus()).isEqualTo(CardStatus.ACTIVE);
        verify(cardRepository, never()).save(any(Card.class));
    }
}