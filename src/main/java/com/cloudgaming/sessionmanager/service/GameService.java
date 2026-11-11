package com.cloudgaming.sessionmanager.service;

import com.cloudgaming.sessionmanager.dto.AdminGameDto;
import com.cloudgaming.sessionmanager.dto.CreateGameRequest;
import com.cloudgaming.sessionmanager.dto.GameDto;
import com.cloudgaming.sessionmanager.dto.UpdateGameRequest;
import com.cloudgaming.sessionmanager.entity.Game;
import com.cloudgaming.sessionmanager.exception.GameAlreadyExistsException;
import com.cloudgaming.sessionmanager.exception.GameNotFoundException;
import com.cloudgaming.sessionmanager.repository.GameRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class GameService {

    private final GameRepository gameRepository;

    public List<GameDto> getActiveGames() {
        return gameRepository.findByIsActiveTrue().stream()
                .map(game -> new GameDto(
                        game.getSlug(),
                        game.getTitle(),
                        game.getRatePerMinute(),
                        game.getMinBlockMinutes(),
                        game.getSupportsKeyboard(),
                        game.getSupportsMouse(),
                        game.getSupportsGamepad()
                ))
                .toList();
    }

    @Transactional
    public AdminGameDto createGame(CreateGameRequest request) {
        if (gameRepository.existsBySlug(request.slug())) {
            throw new GameAlreadyExistsException("Игра с slug '" + request.slug() + "' уже существует");
        }

        Game game = new Game(
                UUID.randomUUID(),
                request.slug(),
                request.title(),
                request.dockerImage(),
                request.ratePerMinute(),
                request.minBlockMinutes(),
                request.supportsKeyboard() != null ? request.supportsKeyboard() : true,
                request.supportsMouse() != null ? request.supportsMouse() : true,
                request.supportsGamepad() != null ? request.supportsGamepad() : false,
                request.isActive() != null ? request.isActive() : true
        );
        return toAdminDto(gameRepository.save(game));
    }

    @Transactional
    public AdminGameDto updateGame(UUID id, UpdateGameRequest request) {
        Game game = gameRepository.findById(id)
                .orElseThrow(() -> new GameNotFoundException("Игра с id " + id + " не найдена"));

        if (request.title() != null) game.setTitle(request.title());
        if (request.ratePerMinute() != null) game.setRatePerMinute(request.ratePerMinute());
        if (request.minBlockMinutes() != null) game.setMinBlockMinutes(request.minBlockMinutes());
        if (request.supportsKeyboard() != null) game.setSupportsKeyboard(request.supportsKeyboard());
        if (request.supportsMouse() != null) game.setSupportsMouse(request.supportsMouse());
        if (request.supportsGamepad() != null) game.setSupportsGamepad(request.supportsGamepad());
        if (request.isActive() != null) game.setIsActive(request.isActive());

        return toAdminDto(gameRepository.save(game));
    }

    private AdminGameDto toAdminDto(Game game) {
        return new AdminGameDto(
                game.getId(), game.getSlug(), game.getTitle(), game.getDockerImage(),
                game.getRatePerMinute(), game.getMinBlockMinutes(),
                game.getSupportsKeyboard(), game.getSupportsMouse(),
                game.getSupportsGamepad(), game.getIsActive()
        );
    }
}
