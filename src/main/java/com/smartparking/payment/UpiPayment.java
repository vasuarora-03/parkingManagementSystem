package com.smartparking.payment;

import com.smartparking.model.PaymentMethodType;

import java.util.UUID;

/** Simulates UPI: the VPA must look like name@bank. */
public class UpiPayment implements PaymentMethod {
    private final String vpa;

    public UpiPayment(String vpa) {
        this.vpa = vpa;
    }

    @Override
    public PaymentResult pay(double amount) {
        if (vpa == null || !vpa.matches("[\\w.\\-]+@[\\w]+")) {
            return PaymentResult.failure("Invalid UPI VPA: expected format name@bank");
        }
        return PaymentResult.success("UPI-" + UUID.randomUUID());
    }

    @Override
    public PaymentMethodType getType() {
        return PaymentMethodType.UPI;
    }
}