package org.example.bank.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class TransferRequest {

    @NotNull(message = "From card ID is required")
    private Long fromCardId;

    @NotNull(message = "To card ID is required")
    private Long toCardId;

    @NotNull(message = "Amount is required")
    @Min(value = 1, message = "Amount must be greater than 0")
    private BigDecimal amount;
}