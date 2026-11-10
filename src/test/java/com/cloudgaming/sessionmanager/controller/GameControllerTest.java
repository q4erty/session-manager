package com.cloudgaming.sessionmanager.controller;

import com.cloudgaming.sessionmanager.config.KeycloakRoleConverter;
import com.cloudgaming.sessionmanager.config.SecurityConfig;
import com.cloudgaming.sessionmanager.dto.GameDto;
import com.cloudgaming.sessionmanager.service.GameService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GameController.class)
@Import({SecurityConfig.class, KeycloakRoleConverter.class})
class GameControllerTest {

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
    void getActiveGames_withPlayerRole_returnsOk() throws Exception {
        when(gameService.getActiveGames()).thenReturn(List.of(
                new GameDto("xonotic", "Xonotic", BigDecimal.ONE, 1, true, true, false)
        ));

        mockMvc.perform(get("/api/v1/games")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_PLAYER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].slug").value("xonotic"))
                .andExpect(jsonPath("$[0].title").value("Xonotic"));
    }

    @Test
    void getActiveGames_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/games"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getActiveGames_withAdminOnlyRole_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/games")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isForbidden());
    }
}
