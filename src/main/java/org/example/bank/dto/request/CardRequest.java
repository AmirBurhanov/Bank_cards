package org.example.bank.dto.request;

import jakarta.validation.constraints.Min;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CardRequest {

    @Min(value = 0, message = "Initial balance cannot be negative")
    private BigDecimal initialBalance;
}