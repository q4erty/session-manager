package com.cloudgaming.sessionmanager.runtime;

/**
 * Описатель запущенного воркера.
 *
 * @param containerId    идентификатор контейнера
 * @param name           имя контейнера
 * @param advertisedHost адрес, по которому воркер доступен
 * @param webrtcPort     порт медиа
 */
public record WorkerHandle(String containerId, String name, String advertisedHost, int webrtcPort) {
}
