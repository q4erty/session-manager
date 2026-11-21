package com.cloudgaming.sessionmanager.exception;

public class WorkerNotReadyException extends RuntimeException {
    public WorkerNotReadyException(String message) {
        super(message);
    }
}
