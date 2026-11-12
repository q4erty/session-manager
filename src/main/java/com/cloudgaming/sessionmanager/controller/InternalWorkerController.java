package com.cloudgaming.sessionmanager.controller;

import com.cloudgaming.sessionmanager.dto.StartWorkerRequest;
import com.cloudgaming.sessionmanager.dto.WorkerHandleDto;
import com.cloudgaming.sessionmanager.service.WorkerDevService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Временные dev-эндпоинты проверки рантайма воркеров.
 * Защита — заголовок X-Internal-Secret, не JWT. Будут удалены после появления сессий.
 */
@RestController
@RequestMapping("/internal/dev/workers")
@RequiredArgsConstructor
public class InternalWorkerController {

    private final WorkerDevService workerDevService;

    @PostMapping("/start")
    public ResponseEntity<WorkerHandleDto> start(@Valid @RequestBody StartWorkerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(workerDevService.startWorker(request.gameSlug()));
    }

    @PostMapping("/{containerId}/stop")
    public ResponseEntity<Void> stop(@PathVariable String containerId) {
        workerDevService.stopWorker(containerId);
        return ResponseEntity.noContent().build();
    }
}
