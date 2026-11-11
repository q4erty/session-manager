package com.cloudgaming.sessionmanager.exception;

public class GameAlreadyExistsException extends RuntimeException {
    public GameAlreadyExistsException(String message) {
        super(message);
    }
}
