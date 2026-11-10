package com.cloudgaming.sessionmanager.controller;

import com.cloudgaming.sessionmanager.dto.GameDto;
import com.cloudgaming.sessionmanager.service.GameService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/games")
public class GameController {

    private final GameService gameService;

    public GameController(GameService gameService) {
        this.gameService = gameService;
    }

    @GetMapping
    @PreAuthorize("hasRole('PLAYER')")
    public List<GameDto> getActiveGames() {
        return gameService.getActiveGames();
    }
}
