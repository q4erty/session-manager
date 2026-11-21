package com.cloudgaming.sessionmanager.exception;

public class WorkerStopFailedException extends RuntimeException {
    public WorkerStopFailedException(String message) {
        super(message);
    }
}
