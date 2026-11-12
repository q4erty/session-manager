package com.cloudgaming.sessionmanager.controller;

import com.cloudgaming.sessionmanager.config.KeycloakRoleConverter;
import com.cloudgaming.sessionmanager.config.SecurityConfig;
import com.cloudgaming.sessionmanager.dto.WorkerHandleDto;
import com.cloudgaming.sessionmanager.exception.GameNotFoundException;
import com.cloudgaming.sessionmanager.exception.GlobalExceptionHandler;
import com.cloudgaming.sessionmanager.exception.NoCapacityException;
import com.cloudgaming.sessionmanager.exception.WorkerNotFoundException;
import com.cloudgaming.sessionmanager.service.WorkerDevService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InternalWorkerController.class)
@Import({SecurityConfig.class, KeycloakRoleConverter.class, GlobalExceptionHandler.class})
@TestPropertySource(properties = "app.security.internal-secret=test-secret")
class InternalWorkerControllerTest {

    private static final String SECRET_HEADER = "X-Internal-Secret";
    private static final String BODY = "{\"gameSlug\":\"xonotic\"}";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    WorkerDevService workerDevService;

    @MockitoBean
    JwtDecoder jwtDecoder;

    @Test
    void start_withoutSecret_returns401() throws Exception {
        mockMvc.perform(post("/internal/dev/workers/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("INTERNAL_SECRET_INVALID"));
    }

    @Test
    void start_withWrongSecret_returns401() throws Exception {
        mockMvc.perform(post("/internal/dev/workers/start")
                        .header(SECRET_HEADER, "wrong")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("INTERNAL_SECRET_INVALID"));
    }

    @Test
    void start_withValidSecret_returns201() throws Exception {
        when(workerDevService.startWorker("xonotic"))
                .thenReturn(new WorkerHandleDto("c-1", "cg-worker-12345678", "127.0.0.1", 9090));

        mockMvc.perform(post("/internal/dev/workers/start")
                        .header(SECRET_HEADER, "test-secret")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.containerId").value("c-1"))
                .andExpect(jsonPath("$.webrtcPort").value(9090));
    }

    @Test
    void start_whenSlotBusy_returns409() throws Exception {
        when(workerDevService.startWorker(anyString())).thenThrow(new NoCapacityException("занят"));

        mockMvc.perform(post("/internal/dev/workers/start")
                        .header(SECRET_HEADER, "test-secret")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("NO_CAPACITY"));
    }

    @Test
    void start_unknownGame_returns404() throws Exception {
        when(workerDevService.startWorker(anyString())).thenThrow(new GameNotFoundException("нет игры"));

        mockMvc.perform(post("/internal/dev/workers/start")
                        .header(SECRET_HEADER, "test-secret")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("GAME_NOT_FOUND"));
    }

    @Test
    void start_invalidSlug_returns400() throws Exception {
        mockMvc.perform(post("/internal/dev/workers/start")
                        .header(SECRET_HEADER, "test-secret")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"gameSlug\":\"Bad Slug!\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void stop_unknownContainer_returns404() throws Exception {
        doThrow(new WorkerNotFoundException("нет контейнера")).when(workerDevService).stopWorker("nope");

        mockMvc.perform(post("/internal/dev/workers/nope/stop")
                        .header(SECRET_HEADER, "test-secret"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("WORKER_NOT_FOUND"));
    }

    @Test
    void stop_existingContainer_returns204() throws Exception {
        mockMvc.perform(post("/internal/dev/workers/c-1/stop")
                        .header(SECRET_HEADER, "test-secret"))
                .andExpect(status().isNoContent());
    }
}
