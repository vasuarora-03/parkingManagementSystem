package com.smartparking.payment;

/** Outcome of one PaymentMethod.pay() attempt — never throws; PaymentService decides what a failure means. */
public class PaymentResult {
    private final boolean success;
    private final String message;
    private final String referenceId;

    private PaymentResult(boolean success, String message, String referenceId) {
        this.success = success;
        this.message = message;
        this.referenceId = referenceId;
    }

    public static PaymentResult success(String referenceId) {
        return new PaymentResult(true, "Payment successful", referenceId);
    }

    public static PaymentResult failure(String message) {
        return new PaymentResult(false, message, null);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public String getReferenceId() {
        return referenceId;
    }
}