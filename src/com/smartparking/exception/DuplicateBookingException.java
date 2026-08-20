package com.smartparking.exception;

/** Thrown when a vehicle already has an active reservation or open ticket. */
public class DuplicateBookingException extends RuntimeException {
    public DuplicateBookingException(String message) {
        super(message);
    }
}