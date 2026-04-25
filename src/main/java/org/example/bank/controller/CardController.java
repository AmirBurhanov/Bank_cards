package org.example.bank.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.bank.dto.request.CardRequest;
import org.example.bank.dto.request.TransferRequest;
import org.example.bank.dto.response.CardResponse;
import org.example.bank.entity.enums.CardStatus;
import org.example.bank.service.CardService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/cards")
@RequiredArgsConstructor
@Tag(name = "Card Management", description = "Endpoints for managing user cards")
@PreAuthorize("hasRole('USER')")
public class CardController {

    private final CardService cardService;

    @PostMapping
    @Operation(summary = "Create a new card", description = "Creates a new bank card for the authenticated user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Card created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<CardResponse> createCard(@Valid @RequestBody CardRequest request) {
        CardResponse response = cardService.createCard(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "Get user cards", description = "Returns paginated list of user's cards with optional filtering")
    public ResponseEntity<Page<CardResponse>> getMyCards(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) CardStatus status,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<CardResponse> cards = cardService.getMyCards(search, status, pageable);
        return ResponseEntity.ok(cards);
    }

    @GetMapping("/{cardId}/balance")
    @Operation(summary = "Get card balance", description = "Returns the current balance of a specific card")
    public ResponseEntity<BigDecimal> getBalance(@PathVariable Long cardId) {
        BigDecimal balance = cardService.getBalance(cardId);
        return ResponseEntity.ok(balance);
    }

    @PostMapping("/{cardId}/block")
    @Operation(summary = "Block a card", description = "Blocks a specific card, preventing further transactions")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Card blocked successfully"),
            @ApiResponse(responseCode = "404", description = "Card not found"),
            @ApiResponse(responseCode = "403", description = "You don't own this card")
    })
    public ResponseEntity<Void> blockCard(@PathVariable Long cardId) {
        cardService.blockCard(cardId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/transfer")
    @Operation(summary = "Transfer between cards", description = "Transfers money between two cards owned by the same user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Transfer completed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid amount or insufficient funds"),
            @ApiResponse(responseCode = "404", description = "Card not found")
    })
    public ResponseEntity<Void> transfer(@Valid @RequestBody TransferRequest request) {
        cardService.transferBetweenCards(request.getFromCardId(), request.getToCardId(), request.getAmount());
        return ResponseEntity.ok().build();
    }
}