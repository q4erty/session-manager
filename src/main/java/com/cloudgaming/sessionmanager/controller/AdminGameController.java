package com.cloudgaming.sessionmanager.controller;

import com.cloudgaming.sessionmanager.dto.CreateGameRequest;
import com.cloudgaming.sessionmanager.dto.UpdateGameRequest;
import com.cloudgaming.sessionmanager.entity.Game;
import com.cloudgaming.sessionmanager.service.GameService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/games")
public class AdminGameController {

    private final GameService gameService;

    public AdminGameController(GameService gameService) {
        this.gameService = gameService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Game> createGame(@Valid @RequestBody CreateGameRequest request) {
        Game created = gameService.createGame(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Game> updateGame(@PathVariable UUID id, @Valid @RequestBody UpdateGameRequest request) {
        Game updated = gameService.updateGame(id, request);
        return ResponseEntity.ok(updated);
    }
}
