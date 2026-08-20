package com.smartparking.concurrency;

/**
 * Extends Thread rather than implementing Runnable on purpose: there can be many of these
 * running at once (one per vehicle currently plugged in), and EVChargingService needs to hold
 * a reference to each specific one to query its progress or stop it individually. Being a
 * Thread itself (with its own name, its own start()/interrupt()) makes that per-session identity
 * and lifecycle management direct instead of needing a separate wrapper object.
 */
public class EVChargingSession extends Thread {

    private final Long slotId;
    private final long tickIntervalMillis;
    private final int percentPerTick;
    private volatile int chargePercent = 0;
    private volatile boolean charging = true;

    public EVChargingSession(Long slotId, long tickIntervalMillis, int percentPerTick) {
        super("ev-charging-slot-" + slotId);
        this.slotId = slotId;
        this.tickIntervalMillis = tickIntervalMillis;
        this.percentPerTick = percentPerTick;
        setDaemon(true); // a charging session must never keep the JVM alive by itself
    }

    @Override
    public void run() {
        while (charging && chargePercent < 100) {
            try {
                Thread.sleep(tickIntervalMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            chargePercent = Math.min(100, chargePercent + percentPerTick);
        }
    }

    /** Signals the loop to stop after its current tick; stopCharging() then interrupts to wake it immediately. */
    public void stopCharging() {
        charging = false;
    }

    public int getChargePercent() {
        return chargePercent;
    }

    public Long getSlotId() {
        return slotId;
    }
}