package com.cloudgaming.sessionmanager.service;

import com.cloudgaming.sessionmanager.dto.WorkerHandleDto;
import com.cloudgaming.sessionmanager.entity.Game;
import com.cloudgaming.sessionmanager.exception.GameNotFoundException;
import com.cloudgaming.sessionmanager.exception.NoCapacityException;
import com.cloudgaming.sessionmanager.exception.WorkerNotFoundException;
import com.cloudgaming.sessionmanager.exception.WorkerStartFailedException;
import com.cloudgaming.sessionmanager.repository.GameRepository;
import com.cloudgaming.sessionmanager.runtime.CapacityAllocator;
import com.cloudgaming.sessionmanager.runtime.Slot;
import com.cloudgaming.sessionmanager.runtime.StartSpec;
import com.cloudgaming.sessionmanager.runtime.WorkerHandle;
import com.cloudgaming.sessionmanager.runtime.WorkerRuntime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkerDevServiceTest {

    @Mock
    GameRepository gameRepository;
    @Mock
    CapacityAllocator capacityAllocator;
    @Mock
    WorkerRuntime workerRuntime;

    @InjectMocks
    WorkerDevService service;

    private Game game(boolean active) {
        return new Game(UUID.randomUUID(), "xonotic", "Xonotic", "xonotic-worker",
                BigDecimal.ONE, 1, true, true, false, active);
    }

    @Test
    void startWorker_returnsHandleAndKeepsMapping() {
        when(gameRepository.findBySlug("xonotic")).thenReturn(Optional.of(game(true)));
        when(capacityAllocator.tryAllocate(anyString())).thenReturn(Optional.of(new Slot(9090)));
        when(workerRuntime.start(any(StartSpec.class)))
                .thenReturn(new WorkerHandle("c-1", "cg-worker-12345678", "127.0.0.1", 9090));

        WorkerHandleDto dto = service.startWorker("xonotic");

        assertThat(dto.containerId()).isEqualTo("c-1");
        assertThat(dto.webrtcPort()).isEqualTo(9090);
    }

    @Test
    void startWorker_unknownGame_throwsNotFound() {
        when(gameRepository.findBySlug("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.startWorker("missing"))
                .isInstanceOf(GameNotFoundException.class);
        verify(capacityAllocator, never()).tryAllocate(anyString());
    }

    @Test
    void startWorker_inactiveGame_throwsNotFound() {
        when(gameRepository.findBySlug("xonotic")).thenReturn(Optional.of(game(false)));

        assertThatThrownBy(() -> service.startWorker("xonotic"))
                .isInstanceOf(GameNotFoundException.class);
        verify(capacityAllocator, never()).tryAllocate(anyString());
    }

    @Test
    void startWorker_noSlot_throwsNoCapacity() {
        when(gameRepository.findBySlug("xonotic")).thenReturn(Optional.of(game(true)));
        when(capacityAllocator.tryAllocate(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.startWorker("xonotic"))
                .isInstanceOf(NoCapacityException.class);
        verify(workerRuntime, never()).start(any());
    }

    @Test
    void startWorker_runtimeFails_releasesSlot() {
        when(gameRepository.findBySlug("xonotic")).thenReturn(Optional.of(game(true)));
        when(capacityAllocator.tryAllocate(anyString())).thenReturn(Optional.of(new Slot(9090)));
        when(workerRuntime.start(any())).thenThrow(new WorkerStartFailedException("boom"));

        assertThatThrownBy(() -> service.startWorker("xonotic"))
                .isInstanceOf(WorkerStartFailedException.class);
        verify(capacityAllocator).release(anyString());
    }

    @Test
    void stopWorker_unknownContainer_throwsNotFound() {
        assertThatThrownBy(() -> service.stopWorker("nope"))
                .isInstanceOf(WorkerNotFoundException.class);
        verify(workerRuntime, never()).stop(anyString(), any());
    }

    @Test
    void stopWorker_stopsRemovesAndReleasesSlot() {
        when(gameRepository.findBySlug("xonotic")).thenReturn(Optional.of(game(true)));
        when(capacityAllocator.tryAllocate(anyString())).thenReturn(Optional.of(new Slot(9090)));
        when(workerRuntime.start(any()))
                .thenReturn(new WorkerHandle("c-1", "cg-worker-12345678", "127.0.0.1", 9090));
        service.startWorker("xonotic");

        service.stopWorker("c-1");

        verify(workerRuntime).stop(eq("c-1"), eq(Duration.ofSeconds(30)));
        verify(workerRuntime).remove("c-1");
        verify(capacityAllocator).release(anyString());
    }
}
