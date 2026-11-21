package com.cloudgaming.sessionmanager.runtime;

import java.util.Optional;

public interface CapacityAllocator {

    /**
     * Пытается выделить слот для новой игровой комнаты.
     *
     * @param roomId идентификатор игровой комнаты
     * @return выделенный слот либо пусто, если ёмкость исчерпана
     */
    Optional<Slot> tryAllocate(String roomId);

    /**
     * Освобождает слот, ранее выделенный для комнаты.
     */
    void release(String roomId);
}
