package com.cloudgaming.sessionmanager.runtime;

import com.cloudgaming.sessionmanager.config.WorkerProperties;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.transport.DockerHttpClient;
import com.github.dockerjava.zerodep.ZerodepDockerHttpClient;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Проверка на настоящем Docker и образе xonotic-worker.
 * Включается переменной окружения CG_DOCKER_IT=true, в быстрый прогон не входит.
 */
@Tag("docker")
@EnabledIfEnvironmentVariable(named = "CG_DOCKER_IT", matches = "true")
class DockerWorkerRuntimeRealIT {

    @Test
    void startWaitReadyThenStop() {
        DefaultDockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder().build();
        DockerHttpClient http = new ZerodepDockerHttpClient.Builder()
                .dockerHost(config.getDockerHost())
                .sslConfig(config.getSSLConfig())
                .responseTimeout(Duration.ofSeconds(60))
                .build();

        try (DockerClient client = DockerClientImpl.getInstance(config, http)) {
            WorkerProperties props = new WorkerProperties(
                    "unix:///var/run/docker.sock", "127.0.0.1", Duration.ofSeconds(90), false,
                    "4g", 4, null, null, null, null);
            DockerWorkerRuntime runtime = new DockerWorkerRuntime(client, props, new WorkerReadinessProbe());

            String roomId = UUID.randomUUID().toString();
            WorkerHandle handle = runtime.start(new StartSpec("xonotic", roomId, "xonotic-worker:latest"));
            try {
                assertThat(handle.containerId()).isNotBlank();
                assertThat(runtime.isRunning(handle.containerId())).isTrue();
                assertThat(runtime.listManaged()).anyMatch(c -> c.containerId().equals(handle.containerId()));
            } finally {
                runtime.stop(handle.containerId(), Duration.ofSeconds(30));
                runtime.remove(handle.containerId());
            }
            assertThat(runtime.isRunning(handle.containerId())).isFalse();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
