package org.example.bank.dto.response;

import lombok.Builder;
import lombok.Data;
import org.example.bank.entity.enums.CardStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class CardResponse {
    private Long id;
    private String maskedNumber;
    private String ownerName;
    private LocalDate expiryDate;
    private CardStatus status;
    private BigDecimal balance;
}