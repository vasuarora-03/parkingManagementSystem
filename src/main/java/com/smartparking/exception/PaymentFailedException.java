package com.smartparking.exception;

/** Thrown when a PaymentMethod reports a failed attempt (bad card, insufficient balance, etc.). */
public class PaymentFailedException extends RuntimeException {
    public PaymentFailedException(String message) {
        super(message);
    }
}