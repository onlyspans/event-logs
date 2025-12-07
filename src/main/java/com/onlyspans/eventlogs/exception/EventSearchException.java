package com.onlyspans.eventlogs.exception;

public class EventSearchException extends RuntimeException {

    public EventSearchException(String message) {
        super(message);
    }

    public EventSearchException(String message, Throwable cause) {
        super(message, cause);
    }
}
