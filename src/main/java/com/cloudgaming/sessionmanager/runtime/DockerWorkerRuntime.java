package com.cloudgaming.sessionmanager.runtime;

import com.cloudgaming.sessionmanager.config.WorkerProperties;
import com.cloudgaming.sessionmanager.exception.WorkerNotReadyException;
import com.cloudgaming.sessionmanager.exception.WorkerStartFailedException;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.exception.NotModifiedException;
import com.github.dockerjava.api.model.Capability;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Device;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.RestartPolicy;
import com.github.dockerjava.core.command.LogContainerResultCallback;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class DockerWorkerRuntime implements WorkerRuntime {

    private static final int WEBRTC_PORT = SingleSlotCapacityAllocator.WEBRTC_PORT;
    private static final Duration POLL_INTERVAL = Duration.ofMillis(500);
    private static final int CRASH_LOG_LINES = 50;

    private final DockerClient dockerClient;
    private final WorkerProperties properties;
    private final WorkerReadinessProbe readinessProbe;

    @Override
    public WorkerHandle start(StartSpec spec) {
        String name = containerName(spec.roomId());
        String containerId = null;
        try {
            containerId = dockerClient.createContainerCmd(spec.dockerImage())
                    .withName(name)
                    .withLabels(Map.of(
                            "cg.managed", "true",
                            "cg.room_id", spec.roomId(),
                            "cg.game", spec.gameSlug()))
                    .withEnv(buildEnv(spec))
                    .withHostConfig(buildHostConfig())
                    .exec()
                    .getId();

            dockerClient.startContainerCmd(containerId).exec();
            awaitReady(containerId);
        } catch (RuntimeException e) {
            if (containerId != null) {
                cleanupQuietly(containerId);
            }
            if (e instanceof WorkerStartFailedException || e instanceof WorkerNotReadyException) {
                throw e;
            }
            throw new WorkerStartFailedException("Не удалось запустить контейнер: " + e.getMessage(), e);
        }
        return new WorkerHandle(containerId, name, properties.advertisedHost(), WEBRTC_PORT);
    }

    @Override
    public void stop(String containerId, Duration timeout) {
        try {
            dockerClient.stopContainerCmd(containerId)
                    .withTimeout((int) Math.max(1, timeout.toSeconds()))
                    .exec();
        } catch (NotModifiedException alreadyStopped) {
            log.debug("Container {} already stopped", containerId);
        } catch (NotFoundException missing) {
            log.debug("Container {} not found on stop", containerId);
        }
    }

    @Override
    public void remove(String containerId) {
        try {
            dockerClient.removeContainerCmd(containerId)
                    .withForce(true)
                    .withRemoveVolumes(true)
                    .exec();
        } catch (NotFoundException missing) {
            log.debug("Container {} not found on remove", containerId);
        }
    }

    @Override
    public boolean isRunning(String containerId) {
        try {
            Boolean running = dockerClient.inspectContainerCmd(containerId).exec().getState().getRunning();
            return Boolean.TRUE.equals(running);
        } catch (NotFoundException missing) {
            return false;
        }
    }

    @Override
    public List<ManagedContainer> listManaged() {
        List<Container> containers = dockerClient.listContainersCmd()
                .withShowAll(true)
                .withLabelFilter(Map.of("cg.managed", "true"))
                .exec();

        List<ManagedContainer> result = new ArrayList<>();
        for (Container c : containers) {
            Map<String, String> labels = c.getLabels() == null ? Map.of() : c.getLabels();
            String[] names = c.getNames();
            String name = (names != null && names.length > 0) ? names[0] : null;
            result.add(new ManagedContainer(
                    c.getId(),
                    name,
                    labels.get("cg.room_id"),
                    labels.get("cg.game"),
                    "running".equalsIgnoreCase(c.getState())));
        }
        return result;
    }

    private void awaitReady(String containerId) {
        Instant deadline = Instant.now().plus(properties.readyTimeout());
        while (Instant.now().isBefore(deadline)) {
            if (!isRunning(containerId)) {
                String logs = tailLogs(containerId);
                log.error("Worker {} exited before readiness. Last {} lines:\n{}",
                        containerId, CRASH_LOG_LINES, logs);
                throw new WorkerStartFailedException("Контейнер завершился до готовности");
            }
            if (readinessProbe.isReady(properties.advertisedHost(), WEBRTC_PORT)) {
                return;
            }
            sleep();
        }
        String logs = tailLogs(containerId);
        log.error("Worker {} not ready within {}. Last {} lines:\n{}",
                containerId, properties.readyTimeout(), CRASH_LOG_LINES, logs);
        throw new WorkerNotReadyException("Воркер не стал готовым за " + properties.readyTimeout());
    }

    private void sleep() {
        try {
            Thread.sleep(POLL_INTERVAL.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new WorkerStartFailedException("Ожидание готовности прервано", e);
        }
    }

    private String tailLogs(String containerId) {
        StringBuilder buffer = new StringBuilder();
        try (LogContainerResultCallback callback = new LogContainerResultCallback() {
            @Override
            public void onNext(Frame frame) {
                buffer.append(new String(frame.getPayload(), StandardCharsets.UTF_8));
            }
        }) {
            dockerClient.logContainerCmd(containerId)
                    .withStdOut(true)
                    .withStdErr(true)
                    .withTail(CRASH_LOG_LINES)
                    .exec(callback)
                    .awaitCompletion(5, TimeUnit.SECONDS);
            return buffer.toString();
        } catch (Exception e) {
            return "(логи недоступны: " + e.getMessage() + ")";
        }
    }

    private void cleanupQuietly(String containerId) {
        try {
            stop(containerId, Duration.ofSeconds(30));
        } catch (RuntimeException e) {
            log.warn("Не удалось остановить контейнер {} после сбоя запуска", containerId, e);
        }
        try {
            remove(containerId);
        } catch (RuntimeException e) {
            log.error("Не удалось удалить контейнер {} после сбоя запуска — возможна сирота", containerId, e);
        }
    }

    private List<String> buildEnv(StartSpec spec) {
        List<String> env = new ArrayList<>();
        env.add("GAME_ID=" + spec.gameSlug());
        if (properties.stunServer() != null) env.add("STUN_SERVER=" + properties.stunServer());
        if (properties.encoderProfile() != null) env.add("ENCODER_PROFILE=" + properties.encoderProfile());
        if (properties.resolution() != null) env.add("CG_RESOLUTION=" + properties.resolution());
        if (properties.captureFps() != null) env.add("CAPTURE_FPS=" + properties.captureFps());
        return env;
    }

    private HostConfig buildHostConfig() {
        HostConfig hostConfig = HostConfig.newHostConfig()
                .withNetworkMode("host")
                .withMemory(parseMemory(properties.memoryLimit()))
                .withNanoCPUs(properties.cpuLimit() * 1_000_000_000L)
                .withAutoRemove(false)
                .withRestartPolicy(RestartPolicy.noRestart());

        if (properties.uinputEnabled()) {
            hostConfig.withDevices(new Device("rwm", "/dev/uinput", "/dev/uinput"))
                    .withCapAdd(Capability.SYS_ADMIN);
        }
        return hostConfig;
    }

    private static String containerName(String roomId) {
        return "cg-worker-" + roomId.substring(0, Math.min(8, roomId.length()));
    }

    private static long parseMemory(String value) {
        String v = value.trim().toLowerCase(Locale.ROOT);
        long multiplier = 1L;
        if (v.endsWith("g")) {
            multiplier = 1024L * 1024 * 1024;
            v = v.substring(0, v.length() - 1);
        } else if (v.endsWith("m")) {
            multiplier = 1024L * 1024;
            v = v.substring(0, v.length() - 1);
        } else if (v.endsWith("k")) {
            multiplier = 1024L;
            v = v.substring(0, v.length() - 1);
        }
        return (long) (Double.parseDouble(v.trim()) * multiplier);
    }
}
