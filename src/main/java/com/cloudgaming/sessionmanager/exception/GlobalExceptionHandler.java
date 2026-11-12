package com.cloudgaming.sessionmanager.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String GAMES_SLUG_UNIQUE = "games_slug_key";

    private record ErrorResponse(String error, String message) {
    }

    /** 404 — игра не найдена. */
    @ExceptionHandler(GameNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(GameNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("GAME_NOT_FOUND", ex.getMessage()));
    }

    /** 409 — игра с таким slug уже есть. */
    @ExceptionHandler(GameAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleConflict(GameAlreadyExistsException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("GAME_ALREADY_EXISTS", ex.getMessage()));
    }

    /** 409 — свободных слотов воркеров нет. */
    @ExceptionHandler(NoCapacityException.class)
    public ResponseEntity<ErrorResponse> handleNoCapacity(NoCapacityException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("NO_CAPACITY", ex.getMessage()));
    }

    /** 500 — контейнер не запустился или упал до готовности. */
    @ExceptionHandler(WorkerStartFailedException.class)
    public ResponseEntity<ErrorResponse> handleWorkerStartFailed(WorkerStartFailedException ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("WORKER_START_FAILED", ex.getMessage()));
    }

    /** 503 — воркер не подтвердил готовность в срок. */
    @ExceptionHandler(WorkerNotReadyException.class)
    public ResponseEntity<ErrorResponse> handleWorkerNotReady(WorkerNotReadyException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ErrorResponse("WORKER_NOT_READY", ex.getMessage()));
    }

    /** 404 — контейнер не найден. */
    @ExceptionHandler(WorkerNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleWorkerNotFound(WorkerNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("WORKER_NOT_FOUND", ex.getMessage()));
    }

    /** 401 — внутренний секрет неверный. */
    @ExceptionHandler(InternalSecretInvalidException.class)
    public ResponseEntity<ErrorResponse> handleInternalSecret(InternalSecretInvalidException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse("INTERNAL_SECRET_INVALID", ex.getMessage()));
    }

    /** 409 только при нарушении уникальности games.slug, иначе 400. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex) {
        if (isSlugUniqueViolation(ex)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ErrorResponse("GAME_ALREADY_EXISTS", "Игра с таким slug уже существует"));
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("DATA_INTEGRITY_VIOLATION", "Нарушение ограничений базы данных"));
    }

    /** 400 — ошибки Bean Validation. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("VALIDATION_ERROR", message));
    }

    /** 400 — тело запроса не разбирается. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("INVALID_JSON", "Неверный формат запроса"));
    }

    /** 403 — недостаточно прав. */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse("ACCESS_DENIED", "Недостаточно прав"));
    }

    /** 401 — токен отсутствует или невалиден. */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(AuthenticationException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse("UNAUTHORIZED", "Отсутствует или невалиден токен"));
    }

    private boolean isSlugUniqueViolation(Throwable ex) {
        for (Throwable cause = ex; cause != null && cause != cause.getCause(); cause = cause.getCause()) {
            String message = cause.getMessage();
            if (message != null && message.contains(GAMES_SLUG_UNIQUE)) {
                return true;
            }
        }
        return false;
    }
}
