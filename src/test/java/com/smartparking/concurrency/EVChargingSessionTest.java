package com.smartparking.concurrency;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises EVChargingSession directly (short tick intervals instead of the service's real
 * 1s/5% constants) so these run fast while still covering: overshoot capping at 100%, early
 * stop leaving a partial charge, and the thread actually terminating in both cases.
 */
class EVChargingSessionTest {

    @Test
    void chargePercentCapsAtOneHundredEvenWhenTickWouldOvershoot() throws InterruptedException {
        // 30% per tick never lands exactly on 100 (30, 60, 90, 120) -- the last tick must clamp, not overshoot.
        EVChargingSession session = new EVChargingSession(1L, 5, 30);
        session.start();
        session.join(2000);

        assertFalse(session.isAlive(), "session thread should exit once fully charged");
        assertEquals(100, session.getChargePercent(), "charge should clamp at 100, never overshoot past it");
    }

    @Test
    void stopChargingBeforeFullLeavesPartialChargeAndTerminatesThread() throws InterruptedException {
        EVChargingSession session = new EVChargingSession(2L, 50, 5);
        session.start();
        Thread.sleep(120); // let a couple of ticks land, well short of 100%

        session.stopCharging();
        session.interrupt();
        session.join(2000);

        assertFalse(session.isAlive(), "stopCharging + interrupt should wake the sleep and exit the loop promptly");
        int percent = session.getChargePercent();
        assertTrue(percent >= 0 && percent < 100, "stopped early: charge should be partial, got " + percent);
    }

    @Test
    void getSlotIdReturnsTheSlotItWasCreatedFor() {
        EVChargingSession session = new EVChargingSession(42L, 1000, 5);
        assertEquals(42L, session.getSlotId());
    }

    @Test
    void stoppingBeforeAnyTickLeavesChargeAtZero() throws InterruptedException {
        EVChargingSession session = new EVChargingSession(3L, 5000, 5);
        session.start();
        // Stop essentially immediately, before the first tick's sleep could ever complete.
        session.stopCharging();
        session.interrupt();
        session.join(2000);

        assertFalse(session.isAlive());
        assertEquals(0, session.getChargePercent(), "no tick had a chance to land, so charge should still be 0");
    }
}
