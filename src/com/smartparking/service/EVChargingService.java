package com.smartparking.service;

import com.smartparking.concurrency.EVChargingSession;
import com.smartparking.model.Payment;
import com.smartparking.payment.PaymentMethod;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * startCharging/stopCharging(slotId) match the spec; startCharging also takes the ticketId so
 * stopCharging can bill the charging fee against the right Ticket later — Payment's only foreign
 * key is ticketId, and a charging session has no ticket of its own, so it rides on the parking
 * Ticket already open for that slot.
 */
public class EVChargingService {

    private static final long TICK_INTERVAL_MILLIS = 1000;
    private static final int PERCENT_PER_TICK = 5;
    private static final double RATE_PER_PERCENT = 0.10; // simulated cost per % of charge delivered

    private final PaymentService paymentService;
    private final Map<Long, EVChargingSession> activeSessions = new ConcurrentHashMap<>();
    private final Map<Long, Long> slotToTicket = new ConcurrentHashMap<>();

    public EVChargingService(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    public void startCharging(Long ticketId, Long slotId) {
        if (activeSessions.containsKey(slotId)) {
            throw new IllegalStateException("Slot " + slotId + " is already charging");
        }
        EVChargingSession session = new EVChargingSession(slotId, TICK_INTERVAL_MILLIS, PERCENT_PER_TICK);
        activeSessions.put(slotId, session);
        slotToTicket.put(slotId, ticketId);
        session.start();
    }

    public Payment stopCharging(Long slotId, PaymentMethod paymentMethod) {
        EVChargingSession session = activeSessions.remove(slotId);
        Long ticketId = slotToTicket.remove(slotId);
        if (session == null || ticketId == null) {
            throw new IllegalArgumentException("No active charging session for slot " + slotId);
        }
        session.stopCharging();
        session.interrupt(); // wake it immediately instead of waiting out its current sleep tick

        double fee = session.getChargePercent() * RATE_PER_PERCENT;
        return paymentService.pay(ticketId, fee, paymentMethod);
    }

    public int getChargePercent(Long slotId) {
        EVChargingSession session = activeSessions.get(slotId);
        return session != null ? session.getChargePercent() : 0;
    }
}