package com.cloudgaming.sessionmanager.runtime;

import java.time.Duration;
import java.util.List;

public interface WorkerRuntime {

    /**
     * Запускает контейнер воркера и дожидается его готовности.
     * При неудаче контейнер удаляется, наружу уходит доменное исключение.
     *
     * @param spec спецификация запуска
     * @return описатель запущенного воркера
     */
    WorkerHandle start(StartSpec spec);

    /**
     * Останавливает контейнер: мягко, затем принудительно по истечении таймаута.
     *
     * @param containerId идентификатор контейнера
     * @param timeout     максимальное ожидание мягкой остановки
     */
    void stop(String containerId, Duration timeout);

    /**
     * Удаляет контейнер.
     *
     * @param containerId идентификатор контейнера
     */
    void remove(String containerId);

    /**
     * Проверяет, работает ли контейнер.
     *
     * @param containerId идентификатор контейнера
     * @return true если контейнер в состоянии "работает"
     */
    boolean isRunning(String containerId);

    /**
     * Возвращает контейнеры с меткой cg.managed=true.
     *
     * @return список управляемых контейнеров
     */
    List<ManagedContainer> listManaged();
}
