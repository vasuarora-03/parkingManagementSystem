package com.smartparking.payment;

import com.smartparking.model.PaymentMethodType;

import java.util.UUID;

/** Cash paid in person, no external gateway to fail against — always succeeds. */
public class CashPayment implements PaymentMethod {
    @Override
    public PaymentResult pay(double amount) {
        return PaymentResult.success("CASH-" + UUID.randomUUID());
    }

    @Override
    public PaymentMethodType getType() {
        return PaymentMethodType.CASH;
    }
}