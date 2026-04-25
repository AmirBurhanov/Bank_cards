package org.example.bank.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.bank.dto.request.CardRequest;
import org.example.bank.dto.response.CardResponse;
import org.example.bank.entity.Card;
import org.example.bank.entity.User;
import org.example.bank.entity.enums.CardStatus;
import org.example.bank.exception.CardNotFoundException;
import org.example.bank.exception.InsufficientFundsException;
import org.example.bank.exception.UnauthorizedAccessException;
import org.example.bank.repository.CardRepository;
import org.example.bank.repository.UserRepository;
import org.example.bank.utils.CardNumberGenerator;
import org.example.bank.utils.CardNumberMasker;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class CardService {

    private final CardRepository cardRepository;
    private final UserRepository userRepository;
    private final CardNumberGenerator cardNumberGenerator;
    private final CardNumberMasker cardNumberMasker;

    @Transactional
    public CardResponse createCard(CardRequest request) {
        User currentUser = getCurrentUser();

        String encryptedCardNumber = cardNumberGenerator.generateEncryptedNumber();
        String maskedNumber = cardNumberMasker.mask(encryptedCardNumber);

        Card card = Card.builder()
                .encryptedCardNumber(encryptedCardNumber)
                .maskedNumber(maskedNumber)
                .owner(currentUser)
                .expiryDate(LocalDate.now().plusYears(3))
                .status(CardStatus.ACTIVE)
                .balance(request.getInitialBalance() != null ? request.getInitialBalance() : BigDecimal.ZERO)
                .build();

        Card savedCard = cardRepository.save(card);
        log.info("Created new card for user: {}", currentUser.getUsername());

        return mapToResponse(savedCard);
    }

    @Transactional(readOnly = true)
    public Page<CardResponse> getMyCards(String search, CardStatus status, Pageable pageable) {
        User currentUser = getCurrentUser();

        Page<Card> cards;
        if (status != null) {
            cards = cardRepository.findByOwnerAndStatus(currentUser, status, pageable);
        } else if (search != null && !search.isEmpty()) {
            cards = cardRepository.searchByOwnerAndMaskedNumber(currentUser, search, pageable);
        } else {
            cards = cardRepository.findByOwner(currentUser, pageable);
        }

        return cards.map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public BigDecimal getBalance(Long cardId) {
        User currentUser = getCurrentUser();
        Card card = getCardAndValidateOwner(cardId, currentUser);
        return card.getBalance();
    }

    @Transactional
    public void blockCard(Long cardId) {
        User currentUser = getCurrentUser();
        Card card = getCardAndValidateOwner(cardId, currentUser);

        if (card.getStatus() == CardStatus.BLOCKED) {
            throw new IllegalStateException("Card is already blocked");
        }

        card.setStatus(CardStatus.BLOCKED);
        cardRepository.save(card);
        log.info("Blocked card {} for user {}", cardId, currentUser.getUsername());
    }

    @Transactional
    public void activateCard(Long cardId) {
        User currentUser = getCurrentUser();
        Card card = getCardAndValidateOwner(cardId, currentUser);

        if (card.getStatus() == CardStatus.ACTIVE) {
            throw new IllegalStateException("Card is already active");
        }

        if (card.getExpiryDate().isBefore(LocalDate.now())) {
            throw new IllegalStateException("Cannot activate expired card");
        }

        card.setStatus(CardStatus.ACTIVE);
        cardRepository.save(card);
        log.info("Activated card {} for user {}", cardId, currentUser.getUsername());
    }

    @Transactional(readOnly = true)
    public Page<CardResponse> getAllCards(Pageable pageable) {
        return cardRepository.findAll(pageable).map(this::mapToResponse);
    }

    @Transactional
    public void deleteCard(Long cardId) {
        User currentUser = getCurrentUser();
        Card card = getCardAndValidateOwner(cardId, currentUser);

        cardRepository.delete(card);
        log.info("Deleted card {} for user {}", cardId, currentUser.getUsername());
    }

    @Transactional
    public void transferBetweenCards(Long fromCardId, Long toCardId, BigDecimal amount) {
        if (amount == null) {
            throw new IllegalArgumentException("Transfer amount cannot be null");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Transfer amount must be positive");
        }

        if (fromCardId == null || toCardId == null) {
            throw new IllegalArgumentException("Card IDs cannot be null");
        }

        if (fromCardId.equals(toCardId)) {
            throw new IllegalArgumentException("Cannot transfer to the same card");
        }

        User currentUser = getCurrentUser();

        Card fromCard = getCardAndValidateOwner(fromCardId, currentUser);
        Card toCard = getCardAndValidateOwner(toCardId, currentUser);

        if (fromCard.getStatus() != CardStatus.ACTIVE) {
            throw new IllegalStateException("Source card is not active. Status: " + fromCard.getStatus());
        }

        if (toCard.getStatus() != CardStatus.ACTIVE) {
            throw new IllegalStateException("Destination card is not active. Status: " + toCard.getStatus());
        }

        if (fromCard.getExpiryDate().isBefore(LocalDate.now())) {
            throw new IllegalStateException("Source card has expired on " + fromCard.getExpiryDate());
        }

        if (fromCard.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException(
                    String.format("Insufficient funds. Balance: %.2f, Required: %.2f",
                            fromCard.getBalance(), amount)
            );
        }

        fromCard.setBalance(fromCard.getBalance().subtract(amount));
        toCard.setBalance(toCard.getBalance().add(amount));

        cardRepository.save(fromCard);
        cardRepository.save(toCard);

        log.info("Transferred {} from card {} to card {} for user {}",
                amount, fromCardId, toCardId, currentUser.getUsername());
    }

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private Card getCardAndValidateOwner(Long cardId, User user) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new CardNotFoundException("Card not found with id: " + cardId));

        if (!card.getOwner().getId().equals(user.getId())) {
            throw new UnauthorizedAccessException("You don't own this card");
        }

        return card;
    }

    private CardResponse mapToResponse(Card card) {
        return CardResponse.builder()
                .id(card.getId())
                .maskedNumber(card.getMaskedNumber())
                .ownerName(card.getOwner().getFullName())
                .expiryDate(card.getExpiryDate())
                .status(card.getStatus())
                .balance(card.getBalance())
                .build();
    }
}