package com.cloudgaming.sessionmanager.runtime;

/**
 * @param containerId идентификатор контейнера
 * @param name        имя контейнера
 * @param roomId      идентификатор игровой комнаты
 * @param gameSlug    идентификатор игры
 * @param running     работает ли контейнер
 */
public record ManagedContainer(String containerId, String name, String roomId, String gameSlug, boolean running) {
}
