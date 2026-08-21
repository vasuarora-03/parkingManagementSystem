package com.smartparking.concurrency;

import com.smartparking.service.ReservationService;

import java.time.LocalDateTime;

/**
 * Implements Runnable rather than extending Thread on purpose: this is a single, reusable
 * "task" — there's exactly one monitor for the whole app, started once at startup on a Thread
 * that Main owns and controls. Runnable keeps this class free to be handed to whatever runs it
 * (a plain Thread today, an ExecutorService or @Scheduled method later) without being a Thread
 * itself. Contrast with EVChargingSession below, which extends Thread because there are many
 * of them, each with its own independent identity that needs to be held onto and stopped by
 * reference — being a Thread is the natural shape there.
 *
 * WHY a periodic sweep instead of checking expiry lazily wherever a reservation is read: a
 * reservation nobody ever queries again (a no-show) would otherwise keep its slot held as
 * RESERVED forever, silently shrinking the lot's real capacity. A background sweep guarantees
 * expired holds get released even if no one ever asks about that specific reservation again.
 *
 * Known, accepted limitation: this shares the same class of race as §8 (a customer could call
 * ReservationService.confirm() at almost the exact instant this sweep decides to expire the same
 * reservation), just far lower-probability since it requires the sweep and a live CLI action to
 * land on the same reservation in the same instant. The spec's concurrency focus is the slot
 * allocation race; the same synchronized-block treatment could be extended to reservation status
 * transitions if this became a real concern.
 */
public class ReservationExpiryMonitor implements Runnable {

    private final ReservationService reservationService;
    private final long pollIntervalMillis;
    private volatile boolean running = true;

    public ReservationExpiryMonitor(ReservationService reservationService, long pollIntervalMillis) {
        this.reservationService = reservationService;
        this.pollIntervalMillis = pollIntervalMillis;
    }

    @Override
    public void run() {
        while (running) {
            reservationService.findAllPending().stream()
                    .filter(r -> LocalDateTime.now().isAfter(r.getExpiresAt()))
                    .forEach(reservationService::expire);
            try {
                Thread.sleep(pollIntervalMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                running = false;
            }
        }
    }

    /** Lets Main stop the sweep cleanly on shutdown rather than relying only on the daemon flag. */
    public void stop() {
        running = false;
    }
}