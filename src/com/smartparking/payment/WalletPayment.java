package com.smartparking.payment;

import com.smartparking.model.PaymentMethodType;

import java.util.UUID;

/** Simulates an in-app wallet: fails if the amount exceeds the wallet's known balance. */
public class WalletPayment implements PaymentMethod {
    private final double balance;

    public WalletPayment(double balance) {
        this.balance = balance;
    }

    @Override
    public PaymentResult pay(double amount) {
        if (amount > balance) {
            return PaymentResult.failure("Insufficient wallet balance: has " + balance + ", needs " + amount);
        }
        return PaymentResult.success("WALLET-" + UUID.randomUUID());
    }

    @Override
    public PaymentMethodType getType() {
        return PaymentMethodType.WALLET;
    }
}