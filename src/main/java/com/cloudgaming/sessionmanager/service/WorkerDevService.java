package com.cloudgaming.sessionmanager.service;

import com.cloudgaming.sessionmanager.dto.WorkerHandleDto;
import com.cloudgaming.sessionmanager.entity.Game;
import com.cloudgaming.sessionmanager.exception.GameNotFoundException;
import com.cloudgaming.sessionmanager.exception.NoCapacityException;
import com.cloudgaming.sessionmanager.exception.WorkerNotFoundException;
import com.cloudgaming.sessionmanager.exception.WorkerStopFailedException;
import com.cloudgaming.sessionmanager.repository.GameRepository;
import com.cloudgaming.sessionmanager.runtime.CapacityAllocator;
import com.cloudgaming.sessionmanager.runtime.ManagedContainer;
import com.cloudgaming.sessionmanager.runtime.StartSpec;
import com.cloudgaming.sessionmanager.runtime.WorkerHandle;
import com.cloudgaming.sessionmanager.runtime.WorkerRuntime;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Временный сервис проверки рантайма воркеров до появления логики сессий.
 * Держит в памяти связку containerId -> roomId, чтобы освобождать слот при остановке.
 */
@Slf4j
@Service
@Profile("dev")
@RequiredArgsConstructor
public class WorkerDevService {

    private static final Duration STOP_TIMEOUT = Duration.ofSeconds(30);

    private final GameRepository gameRepository;
    private final CapacityAllocator capacityAllocator;
    private final WorkerRuntime workerRuntime;

    private final Map<String, String> containerToRoom = new ConcurrentHashMap<>();

    /**
     * Подбирает контейнеры, оставшиеся от прошлого запуска: живой воркер берётся под учёт
     * и занимает слот, остановленный или лишний — удаляется. Ошибки Docker не блокируют старт.
     */
    @PostConstruct
    void reconcileManagedContainers() {
        try {
            for (ManagedContainer container : workerRuntime.listManaged()) {
                String containerId = container.containerId();
                String roomId = container.roomId();
                boolean adopted = container.running()
                        && roomId != null
                        && capacityAllocator.tryAllocate(roomId).isPresent();
                if (adopted) {
                    containerToRoom.put(containerId, roomId);
                    log.warn("Adopted worker after restart: container={}, room={}", containerId, roomId);
                } else {
                    log.warn("Removing stale managed container: container={}, running={}",
                            containerId, container.running());
                    stopAndRemove(containerId);
                }
            }
        } catch (RuntimeException e) {
            log.error("Startup reconciliation of managed containers failed", e);
        }
    }

    /**
     * Запускает воркер для активной игры каталога.
     * Неактивная игра трактуется как отсутствующая (404 GAME_NOT_FOUND).
     *
     * @param gameSlug slug игры
     * @return данные запущенного воркера
     */
    public WorkerHandleDto startWorker(String gameSlug) {
        Game game = gameRepository.findBySlug(gameSlug)
                .filter(g -> Boolean.TRUE.equals(g.getIsActive()))
                .orElseThrow(() -> new GameNotFoundException("Игра не найдена или неактивна: " + gameSlug));

        String roomId = UUID.randomUUID().toString();
        capacityAllocator.tryAllocate(roomId)
                .orElseThrow(() -> new NoCapacityException("Слот воркеров занят"));

        try {
            WorkerHandle handle = workerRuntime.start(
                    new StartSpec(game.getSlug(), roomId, game.getDockerImage()));
            containerToRoom.put(handle.containerId(), roomId);
            log.info("Worker started: container={}, room={}, game={}", handle.containerId(), roomId, gameSlug);
            return new WorkerHandleDto(handle.containerId(), handle.name(), handle.advertisedHost(), handle.webrtcPort());
        } catch (RuntimeException e) {
            capacityAllocator.release(roomId);
            throw e;
        }
    }

    /**
     * Останавливает и удаляет контейнер. Слот и маппинг освобождаются только когда
     * контейнера точно нет — иначе остановку можно повторить.
     *
     * @param containerId идентификатор контейнера
     */
    public void stopWorker(String containerId) {
        String roomId = containerToRoom.get(containerId);
        if (roomId == null) {
            throw new WorkerNotFoundException("Контейнер не найден: " + containerId);
        }

        if (!stopAndRemove(containerId)) {
            log.error("Контейнер {} всё ещё существует: слот и маппинг сохранены для повтора", containerId);
            throw new WorkerStopFailedException("Контейнер не удалён: " + containerId);
        }

        containerToRoom.remove(containerId);
        capacityAllocator.release(roomId);
        log.info("Worker stopped: container={}, room={}", containerId, roomId);
    }

    private boolean stopAndRemove(String containerId) {
        try {
            workerRuntime.stop(containerId, STOP_TIMEOUT);
        } catch (RuntimeException e) {
            log.warn("Не удалось остановить контейнер {}", containerId, e);
        }
        try {
            workerRuntime.remove(containerId);
        } catch (RuntimeException e) {
            log.warn("Не удалось удалить контейнер {}", containerId, e);
        }
        return isAbsent(containerId);
    }

    private boolean isAbsent(String containerId) {
        try {
            return workerRuntime.listManaged().stream()
                    .noneMatch(c -> containerId.equals(c.containerId()));
        } catch (RuntimeException e) {
            log.warn("Не удалось проверить наличие контейнера {}", containerId, e);
            return false;
        }
    }
}