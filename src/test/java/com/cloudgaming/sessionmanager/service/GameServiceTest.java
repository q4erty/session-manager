package com.cloudgaming.sessionmanager.service;

import com.cloudgaming.sessionmanager.dto.CreateGameRequest;
import com.cloudgaming.sessionmanager.dto.GameDto;
import com.cloudgaming.sessionmanager.dto.UpdateGameRequest;
import com.cloudgaming.sessionmanager.entity.Game;
import com.cloudgaming.sessionmanager.exception.GameAlreadyExistsException;
import com.cloudgaming.sessionmanager.exception.GameNotFoundException;
import com.cloudgaming.sessionmanager.repository.GameRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GameServiceTest {

    @Mock
    GameRepository gameRepository;

    @InjectMocks
    GameService gameService;

    @Test
    void getActiveGames_returnsOnlyActiveGames() {
        Game active = new Game(UUID.randomUUID(), "xonotic", "Xonotic", "img",
                BigDecimal.ONE, 1, true, true, false, true);
        Game inactive = new Game(UUID.randomUUID(), "doom", "Doom", "img",
                BigDecimal.ONE, 1, true, true, false, false);
        when(gameRepository.findByIsActiveTrue()).thenReturn(List.of(active));

        List<GameDto> result = gameService.getActiveGames();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).slug()).isEqualTo("xonotic");
    }

    @Test
    void createGame_savesWithDefaults() {
        when(gameRepository.existsBySlug("new-game")).thenReturn(false);
        when(gameRepository.save(any(Game.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateGameRequest request = new CreateGameRequest(
                "new-game", "New Game", "image", BigDecimal.TEN, 5,
                null, null, null, null
        );

        Game saved = gameService.createGame(request);

        assertThat(saved.getSupportsKeyboard()).isTrue();
        assertThat(saved.getSupportsMouse()).isTrue();
        assertThat(saved.getSupportsGamepad()).isFalse();
        assertThat(saved.getIsActive()).isTrue();
        verify(gameRepository).save(any(Game.class));
    }

    @Test
    void createGame_throwsWhenSlugExists() {
        when(gameRepository.existsBySlug("xonotic")).thenReturn(true);

        CreateGameRequest request = new CreateGameRequest(
                "xonotic", "Xonotic", "img", BigDecimal.ONE, 1,
                true, true, false, true
        );

        assertThatThrownBy(() -> gameService.createGame(request))
                .isInstanceOf(GameAlreadyExistsException.class);
    }

    @Test
    void updateGame_updatesOnlyProvidedFields() {
        UUID id = UUID.randomUUID();
        Game existing = new Game(id, "xonotic", "Old Title", "img",
                BigDecimal.ONE, 1, true, true, false, true);
        when(gameRepository.findById(id)).thenReturn(Optional.of(existing));
        when(gameRepository.save(any(Game.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateGameRequest request = new UpdateGameRequest(
                "New Title", null, null, null, null, null, null
        );

        Game updated = gameService.updateGame(id, request);

        assertThat(updated.getTitle()).isEqualTo("New Title");
        assertThat(updated.getRatePerMinute()).isEqualByComparingTo(BigDecimal.ONE);
    }

    @Test
    void updateGame_throwsWhenNotFound() {
        UUID id = UUID.randomUUID();
        when(gameRepository.findById(id)).thenReturn(Optional.empty());

        UpdateGameRequest request = new UpdateGameRequest("X", null, null, null, null, null, null);

        assertThatThrownBy(() -> gameService.updateGame(id, request))
                .isInstanceOf(GameNotFoundException.class);
    }
}
