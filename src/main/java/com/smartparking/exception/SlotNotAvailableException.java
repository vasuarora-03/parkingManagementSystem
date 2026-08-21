package com.smartparking.exception;

/**
 * Thrown when no parking slot of the required type is free. Unchecked (extends RuntimeException)
 * so service method signatures stay clean — the CLI catches this at the boundary today; later
 * a @ControllerAdvice maps it to HTTP 404/409 without any service code changing.
 */
public class SlotNotAvailableException extends RuntimeException {
    public SlotNotAvailableException(String message) {
        super(message);
    }
}