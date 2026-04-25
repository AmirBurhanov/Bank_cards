package org.example.bank.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.bank.dto.response.CardResponse;
import org.example.bank.entity.User;
import org.example.bank.service.CardService;
import org.example.bank.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "Admin Management", description = "Endpoints for administrators")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final CardService cardService;
    private final UserService userService;

    @GetMapping("/cards")
    @Operation(summary = "Get all cards", description = "Returns paginated list of all cards in the system")
    public ResponseEntity<Page<CardResponse>> getAllCards(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<CardResponse> cards = cardService.getAllCards(pageable);
        return ResponseEntity.ok(cards);
    }

    @DeleteMapping("/cards/{cardId}")
    @Operation(summary = "Delete a card", description = "Permanently deletes a card from the system")
    public ResponseEntity<Void> deleteCard(@PathVariable Long cardId) {
        cardService.deleteCard(cardId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/cards/{cardId}/activate")
    @Operation(summary = "Activate a card", description = "Activates a blocked card")
    public ResponseEntity<Void> activateCard(@PathVariable Long cardId) {
        cardService.activateCard(cardId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/users")
    @Operation(summary = "Get all users", description = "Returns list of all users in the system")
    public ResponseEntity<Page<User>> getAllUsers(@PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(userService.getAllUsers(pageable));
    }

    @DeleteMapping("/users/{userId}")
    @Operation(summary = "Delete a user", description = "Permanently deletes a user from the system")
    public ResponseEntity<Void> deleteUser(@PathVariable Long userId) {
        userService.deleteUser(userId);
        return ResponseEntity.ok().build();
    }
}