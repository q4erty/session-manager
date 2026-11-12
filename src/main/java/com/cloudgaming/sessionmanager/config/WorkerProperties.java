package com.cloudgaming.sessionmanager.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * @param dockerHost     адрес Docker-демона
 * @param advertisedHost адрес, по которому воркер доступен извне
 * @param readyTimeout   максимальное ожидание готовности воркера
 * @param uinputEnabled  добавлять ли /dev/uinput и SYS_ADMIN
 * @param memoryLimit    лимит памяти контейнера, например "4g"
 * @param cpuLimit       лимит CPU в ядрах
 * @param stunServer     опциональный STUN-сервер для воркера
 * @param encoderProfile опциональный профиль кодирования
 * @param resolution     опциональное разрешение (CG_RESOLUTION)
 * @param captureFps     опциональный FPS захвата (CAPTURE_FPS)
 */
@ConfigurationProperties(prefix = "app.worker")
public record WorkerProperties(
        String dockerHost,
        String advertisedHost,
        Duration readyTimeout,
        boolean uinputEnabled,
        String memoryLimit,
        int cpuLimit,
        String stunServer,
        String encoderProfile,
        String resolution,
        Integer captureFps
) {
    public WorkerProperties {
        if (dockerHost == null || dockerHost.isBlank()) dockerHost = "unix:///var/run/docker.sock";
        if (advertisedHost == null || advertisedHost.isBlank()) advertisedHost = "127.0.0.1";
        if (readyTimeout == null) readyTimeout = Duration.ofSeconds(90);
        if (memoryLimit == null || memoryLimit.isBlank()) memoryLimit = "4g";
        if (cpuLimit < 1) cpuLimit = 4;
    }
}
