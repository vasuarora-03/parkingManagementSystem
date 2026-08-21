package com.smartparking.payment;

import com.smartparking.model.PaymentMethodType;

import java.util.UUID;

/** Simulates card-gateway validation: card number must be 16 digits, CVV must be 3 digits. */
public class CardPayment implements PaymentMethod {
    private final String cardNumber;
    private final String cvv;

    public CardPayment(String cardNumber, String cvv) {
        this.cardNumber = cardNumber;
        this.cvv = cvv;
    }

    @Override
    public PaymentResult pay(double amount) {
        if (cardNumber == null || !cardNumber.matches("\\d{16}")) {
            return PaymentResult.failure("Invalid card number: must be 16 digits");
        }
        if (cvv == null || !cvv.matches("\\d{3}")) {
            return PaymentResult.failure("Invalid CVV: must be 3 digits");
        }
        return PaymentResult.success("CARD-" + UUID.randomUUID());
    }

    @Override
    public PaymentMethodType getType() {
        return PaymentMethodType.CARD;
    }
}