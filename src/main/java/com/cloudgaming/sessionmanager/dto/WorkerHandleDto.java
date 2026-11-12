package com.cloudgaming.sessionmanager.dto;

/**
 * @param containerId    идентификатор контейнера
 * @param name           имя контейнера
 * @param advertisedHost адрес воркера
 * @param webrtcPort     порт медиа
 */
public record WorkerHandleDto(String containerId, String name, String advertisedHost, int webrtcPort) {
}
