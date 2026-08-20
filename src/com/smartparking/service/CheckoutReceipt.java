package com.smartparking.service;

import com.smartparking.model.Payment;
import com.smartparking.model.Ticket;

/** What a single checkOut() call produces: the closed Ticket (with its fee) and the resulting Payment attempt. */
public class CheckoutReceipt {
    private final Ticket ticket;
    private final Payment payment;

    public CheckoutReceipt(Ticket ticket, Payment payment) {
        this.ticket = ticket;
        this.payment = payment;
    }

    public Ticket getTicket() {
        return ticket;
    }

    public Payment getPayment() {
        return payment;
    }
}