package com.cloudgaming.sessionmanager.controller;

import com.cloudgaming.sessionmanager.config.KeycloakRoleConverter;
import com.cloudgaming.sessionmanager.config.SecurityConfig;
import com.cloudgaming.sessionmanager.dto.AdminGameDto;
import com.cloudgaming.sessionmanager.exception.GameAlreadyExistsException;
import com.cloudgaming.sessionmanager.exception.GameNotFoundException;
import com.cloudgaming.sessionmanager.exception.GlobalExceptionHandler;
import com.cloudgaming.sessionmanager.service.GameService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminGameController.class)
@Import({SecurityConfig.class, KeycloakRoleConverter.class, GlobalExceptionHandler.class})
class AdminGameControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    GameService gameService;

    @MockitoBean
    JwtDecoder jwtDecoder;

    @BeforeEach
    void setUp() {
        when(jwtDecoder.decode(anyString())).thenReturn(
                Jwt.withTokenValue("dummy-token")
                        .header("alg", "none")
                        .claim("sub", "test-user")
                        .claim("realm_access", Map.of("roles", List.of("PLAYER", "ADMIN")))
                        .issuedAt(Instant.now())
                        .expiresAt(Instant.now().plusSeconds(3600))
                        .build()
        );
    }

    @Test
    void createGame_withAdminRole_returns201() throws Exception {
        AdminGameDto created = new AdminGameDto(UUID.randomUUID(), "new-game", "New Game", "image",
                BigDecimal.TEN, 5, true, true, false, true);
        when(gameService.createGame(any())).thenReturn(created);

        String body = """
                {
                  "slug": "new-game",
                  "title": "New Game",
                  "dockerImage": "image",
                  "ratePerMinute": 10.00,
                  "minBlockMinutes": 5
                }
                """;

        mockMvc.perform(post("/api/v1/admin/games")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.slug").value("new-game"));
    }

    @Test
    void createGame_withPlayerRole_returns403() throws Exception {
        String body = """
                {
                  "slug": "x", "title": "x", "dockerImage": "x",
                  "ratePerMinute": 1, "minBlockMinutes": 1
                }
                """;

        mockMvc.perform(post("/api/v1/admin/games")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_PLAYER"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void createGame_withEmptySlug_returns400() throws Exception {
        String body = """
                {
                  "slug": "",
                  "title": "New Game",
                  "dockerImage": "image",
                  "ratePerMinute": 10,
                  "minBlockMinutes": 5
                }
                """;

        mockMvc.perform(post("/api/v1/admin/games")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createGame_withNegativeRate_returns400() throws Exception {
        String body = """
                {
                  "slug": "x", "title": "x", "dockerImage": "x",
                  "ratePerMinute": -5, "minBlockMinutes": 1
                }
                """;

        mockMvc.perform(post("/api/v1/admin/games")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createGame_withZeroMinBlock_returns400() throws Exception {
        String body = """
                {
                  "slug": "x", "title": "x", "dockerImage": "x",
                  "ratePerMinute": 1, "minBlockMinutes": 0
                }
                """;

        mockMvc.perform(post("/api/v1/admin/games")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createGame_withDuplicateSlug_returns409() throws Exception {
        when(gameService.createGame(any()))
                .thenThrow(new GameAlreadyExistsException("slug exists"));

        String body = """
                {
                  "slug": "xonotic", "title": "X", "dockerImage": "x",
                  "ratePerMinute": 1, "minBlockMinutes": 1
                }
                """;

        mockMvc.perform(post("/api/v1/admin/games")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("GAME_ALREADY_EXISTS"));
    }

    @Test
    void updateGame_withAdminRole_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        AdminGameDto updated = new AdminGameDto(id, "xonotic", "Updated", "img",
                BigDecimal.ONE, 1, true, true, false, true);
        when(gameService.updateGame(eq(id), any())).thenReturn(updated);

        String body = """
                {"title": "Updated"}
                """;

        mockMvc.perform(patch("/api/v1/admin/games/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated"));
    }

    @Test
    void updateGame_notFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(gameService.updateGame(eq(id), any()))
                .thenThrow(new GameNotFoundException("not found"));

        String body = """
                {"title": "X"}
                """;

        mockMvc.perform(patch("/api/v1/admin/games/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("GAME_NOT_FOUND"));
    }

    @Test
    void updateGame_withBlankTitle_returns400() throws Exception {
        mockMvc.perform(patch("/api/v1/admin/games/" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"   \"}")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
        verify(gameService, never()).updateGame(any(), any());
    }

    @Test
    void updateGame_withZeroRate_returns400() throws Exception {
        mockMvc.perform(patch("/api/v1/admin/games/" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ratePerMinute\":0}")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
        verify(gameService, never()).updateGame(any(), any());
    }

    @Test
    void updateGame_withZeroMinBlock_returns400() throws Exception {
        mockMvc.perform(patch("/api/v1/admin/games/" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"minBlockMinutes\":0}")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
        verify(gameService, never()).updateGame(any(), any());
    }
}
