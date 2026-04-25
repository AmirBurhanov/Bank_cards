package org.example.bank.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.bank.entity.Card;
import org.example.bank.entity.enums.CardStatus;
import org.example.bank.repository.CardRepository;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class CardExpirationScheduler {

    private final CardRepository cardRepository;

    @Scheduled(cron = "0 0 0 * * *") // Каждый день в полночь
    @Transactional
    public void updateExpiredCards() {
        List<Card> activeCards = cardRepository.findAll().stream()
                .filter(card -> card.getStatus() == CardStatus.ACTIVE)
                .filter(card -> card.getExpiryDate().isBefore(LocalDate.now()))
                .toList();

        for (Card card : activeCards) {
            card.setStatus(CardStatus.EXPIRED);
            cardRepository.save(card);
            log.info("Card {} expired and status updated to EXPIRED", card.getId());
        }
    }
}