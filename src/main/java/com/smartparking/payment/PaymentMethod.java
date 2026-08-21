package com.smartparking.payment;

import com.smartparking.model.PaymentMethodType;

/**
 * Why this interface exists: card, UPI, wallet, and cash each have genuinely different
 * validation and failure logic (a card has format/limit checks, a wallet has a balance check,
 * cash has no external gateway at all and can't meaningfully "fail"). PaymentService calls
 * pay(amount) without knowing which of these it holds. Adding a new method later (e.g. NetBanking)
 * is one new class here — PaymentService is never touched.
 */
public interface PaymentMethod {
    PaymentResult pay(double amount);

    /** Lets PaymentService tag the persisted Payment record with the right enum without instanceof checks. */
    PaymentMethodType getType();
}