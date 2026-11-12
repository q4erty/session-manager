package com.cloudgaming.sessionmanager.runtime;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class SingleSlotCapacityAllocatorTest {

    private final SingleSlotCapacityAllocator allocator = new SingleSlotCapacityAllocator();

    @Test
    void tryAllocate_returnsSlotOnPort9090() {
        Optional<Slot> slot = allocator.tryAllocate("room-1");

        assertThat(slot).isPresent();
        assertThat(slot.orElseThrow().port()).isEqualTo(9090);
    }

    @Test
    void tryAllocate_secondCallReturnsEmpty() {
        allocator.tryAllocate("room-1");

        assertThat(allocator.tryAllocate("room-2")).isEmpty();
    }

    @Test
    void release_allowsAllocateAgain() {
        allocator.tryAllocate("room-1");
        allocator.release("room-1");

        assertThat(allocator.tryAllocate("room-2")).isPresent();
    }

    @Test
    void release_byForeignRoomDoesNotFreeSlot() {
        allocator.tryAllocate("room-1");
        allocator.release("room-other");

        assertThat(allocator.tryAllocate("room-2")).isEmpty();
    }

    @Test
    void tryAllocate_isThreadSafe() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(10);
        try {
            List<Callable<Boolean>> tasks = IntStream.range(0, 10)
                    .<Callable<Boolean>>mapToObj(i -> () -> allocator.tryAllocate("room-" + i).isPresent())
                    .toList();

            int granted = 0;
            for (Future<Boolean> f : pool.invokeAll(tasks)) {
                if (f.get()) granted++;
            }
            assertThat(granted).isEqualTo(1);
        } finally {
            pool.shutdownNow();
        }
    }
}
