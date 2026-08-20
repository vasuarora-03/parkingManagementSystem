package com.smartparking.service;

import com.smartparking.exception.PaymentFailedException;
import com.smartparking.model.Payment;
import com.smartparking.model.PaymentStatus;
import com.smartparking.payment.PaymentMethod;
import com.smartparking.payment.PaymentResult;
import com.smartparking.repository.PaymentRepository;

import java.time.LocalDateTime;

/**
 * Calls whichever PaymentMethod strategy it's handed, then persists a Payment record either
 * way — a failed attempt is still recorded (audit trail), not silently dropped. Only after
 * persisting does it throw PaymentFailedException, so the caller can react but the record
 * of the attempt always exists.
 */
public class PaymentService {

    private final PaymentRepository paymentRepository;

    public PaymentService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    public Payment pay(Long ticketId, double amount, PaymentMethod method) {
        PaymentResult result = method.pay(amount);

        Payment payment = new Payment(
                ticketId,
                amount,
                method.getType(),
                result.isSuccess() ? PaymentStatus.SUCCESS : PaymentStatus.FAILED,
                LocalDateTime.now());
        payment = paymentRepository.save(payment);

        if (!result.isSuccess()) {
            throw new PaymentFailedException(result.getMessage());
        }
        return payment;
    }
}