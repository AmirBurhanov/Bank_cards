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

        // Карта активна и не просрочена (срок действия через 2 года)
        activeCardNotExpired = Card.builder()
                .id(1L)
                .owner(testUser)
                .status(CardStatus.ACTIVE)
                .expiryDate(LocalDate.now().plusYears(2))
                .build();

        // Карта активна, но просрочена (срок действия вчера)
        activeCardExpired = Card.builder()
                .id(2L)
                .owner(testUser)
                .status(CardStatus.ACTIVE)
                .expiryDate(LocalDate.now().minusDays(1))
                .build();

        // Карта заблокирована и просрочена
        blockedCardExpired = Card.builder()
                .id(3L)
                .owner(testUser)
                .status(CardStatus.BLOCKED)
                .expiryDate(LocalDate.now().minusMonths(1))
                .build();

        // Карта уже в статусе EXPIRED
        expiredCardAlreadyExpired = Card.builder()
                .id(4L)
                .owner(testUser)
                .status(CardStatus.EXPIRED)
                .expiryDate(LocalDate.now().minusDays(5))
                .build();
    }

    @Test
    void updateExpiredCards_ShouldUpdateOnlyActiveAndExpiredCards() {
        // Given
        List<Card> allCards = List.of(
                activeCardNotExpired,
                activeCardExpired,
                blockedCardExpired,
                expiredCardAlreadyExpired
        );

        when(cardRepository.findAll()).thenReturn(allCards);
        when(cardRepository.save(any(Card.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        cardExpirationScheduler.updateExpiredCards();

        // Then
        // Проверяем что только активная просроченная карта была обновлена
        assertThat(activeCardNotExpired.getStatus()).isEqualTo(CardStatus.ACTIVE);
        assertThat(activeCardExpired.getStatus()).isEqualTo(CardStatus.EXPIRED);
        assertThat(blockedCardExpired.getStatus()).isEqualTo(CardStatus.BLOCKED);
        assertThat(expiredCardAlreadyExpired.getStatus()).isEqualTo(CardStatus.EXPIRED);

        // Verify save was called only once (for the expired active card)
        verify(cardRepository, times(1)).save(activeCardExpired);
        verify(cardRepository, never()).save(activeCardNotExpired);
        verify(cardRepository, never()).save(blockedCardExpired);
        verify(cardRepository, never()).save(expiredCardAlreadyExpired);
    }

    @Test
    void updateExpiredCards_ShouldHandleMultipleExpiredCards() {
        // Given
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

        // When
        cardExpirationScheduler.updateExpiredCards();

        // Then
        assertThat(expiredCard1.getStatus()).isEqualTo(CardStatus.EXPIRED);
        assertThat(expiredCard2.getStatus()).isEqualTo(CardStatus.EXPIRED);
        assertThat(expiredCard3.getStatus()).isEqualTo(CardStatus.EXPIRED);

        verify(cardRepository, times(3)).save(any(Card.class));
    }

    @Test
    void updateExpiredCards_ShouldDoNothing_WhenNoExpiredCards() {
        // Given
        List<Card> allCards = List.of(activeCardNotExpired);
        when(cardRepository.findAll()).thenReturn(allCards);

        // When
        cardExpirationScheduler.updateExpiredCards();

        // Then
        assertThat(activeCardNotExpired.getStatus()).isEqualTo(CardStatus.ACTIVE);
        verify(cardRepository, never()).save(any(Card.class));
    }

    @Test
    void updateExpiredCards_ShouldDoNothing_WhenNoCards() {
        // Given
        when(cardRepository.findAll()).thenReturn(List.of());

        // When
        cardExpirationScheduler.updateExpiredCards();

        // Then
        verify(cardRepository, never()).save(any(Card.class));
    }

    @Test
    void updateExpiredCards_ShouldNotUpdateCardsExpiringToday() {
        // Given
        Card cardExpiringToday = Card.builder()
                .id(8L)
                .owner(testUser)
                .status(CardStatus.ACTIVE)
                .expiryDate(LocalDate.now()) // Сегодня срок действия (еще не просрочена)
                .build();

        List<Card> allCards = List.of(cardExpiringToday);
        when(cardRepository.findAll()).thenReturn(allCards);

        // When
        cardExpirationScheduler.updateExpiredCards();

        // Then
        assertThat(cardExpiringToday.getStatus()).isEqualTo(CardStatus.ACTIVE);
        verify(cardRepository, never()).save(any(Card.class));
    }
}