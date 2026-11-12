package com.cloudgaming.sessionmanager.runtime;

/**
 * Спецификация запуска воркера.
 *
 * @param gameSlug    идентификатор игры (например, "xonotic")
 * @param roomId      идентификатор игровой комнаты
 * @param dockerImage имя образа (например, "xonotic-worker")
 */
public record StartSpec(String gameSlug, String roomId, String dockerImage) {
}
