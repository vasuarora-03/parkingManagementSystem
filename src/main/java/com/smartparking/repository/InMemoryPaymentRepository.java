package com.smartparking.repository;

import com.smartparking.model.Payment;

public class InMemoryPaymentRepository extends AbstractInMemoryRepository<Payment> implements PaymentRepository {
    public InMemoryPaymentRepository() {
        super(Payment::getId, Payment::setId);
    }
}