package com.cloudgaming.sessionmanager.exception;

public class WorkerStartFailedException extends RuntimeException {
    public WorkerStartFailedException(String message) {
        super(message);
    }

    public WorkerStartFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
