package com.cloudgaming.sessionmanager.runtime;

import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Ёмкость в один слот: в dev одновременно работает один воркер на порту 9090.
 */
@Component
public class SingleSlotCapacityAllocator implements CapacityAllocator {

    /** Единственный порт воркера в dev-профиле. */
    public static final int WEBRTC_PORT = 9090;

    private final AtomicReference<String> occupiedBy = new AtomicReference<>();

    /**
     * Занимает единственный слот, если он свободен.
     *
     * @param roomId идентификатор игровой комнаты
     * @return слот с портом 9090 либо пусто, если слот занят
     */
    @Override
    public Optional<Slot> tryAllocate(String roomId) {
        return occupiedBy.compareAndSet(null, roomId) ? Optional.of(new Slot(WEBRTC_PORT)) : Optional.empty();
    }

    /**
     * Освобождает слот, только если он принадлежит указанной комнате.
     *
     * @param roomId идентификатор игровой комнаты
     */
    @Override
    public void release(String roomId) {
        occupiedBy.compareAndSet(roomId, null);
    }
}
