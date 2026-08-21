package com.smartparking.model;

import java.time.LocalDateTime;

/** A record of one payment attempt against a ticket (or an EV charging session). */
public class Payment {
    private Long id;
    private Long ticketId;
    private double amount;
    private PaymentMethodType method;
    private PaymentStatus status;
    private LocalDateTime timestamp;

    public Payment(Long ticketId, double amount, PaymentMethodType method, PaymentStatus status, LocalDateTime timestamp) {
        this.ticketId = ticketId;
        this.amount = amount;
        this.method = method;
        this.status = status;
        this.timestamp = timestamp;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTicketId() {
        return ticketId;
    }

    public void setTicketId(Long ticketId) {
        this.ticketId = ticketId;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public PaymentMethodType getMethod() {
        return method;
    }

    public void setMethod(PaymentMethodType method) {
        this.method = method;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentStatus status) {
        this.status = status;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "Payment{id=" + id + ", ticketId=" + ticketId + ", amount=" + amount
                + ", method=" + method + ", status=" + status + "}";
    }
}