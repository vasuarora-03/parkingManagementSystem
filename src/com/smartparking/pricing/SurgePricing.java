package com.smartparking.pricing;

import com.smartparking.model.Ticket;

import java.time.LocalDateTime;

/**
 * A decorator, not a standalone rate: wraps any other PricingStrategy and multiplies its
 * result if checkout falls inside the configured peak window. It doesn't know or care whether
 * it's wrapping HourlyPricing or DailyPricing — that's the Strategy pattern composing with
 * itself, the same reason a real payments/pricing engine layers surcharges on top of a base rate.
 */
public class SurgePricing implements PricingStrategy {
    private final PricingStrategy base;
    private final double multiplier;
    private final int peakStartHour; // inclusive, 24h clock
    private final int peakEndHour;   // exclusive

    public SurgePricing(PricingStrategy base, double multiplier, int peakStartHour, int peakEndHour) {
        this.base = base;
        this.multiplier = multiplier;
        this.peakStartHour = peakStartHour;
        this.peakEndHour = peakEndHour;
    }

    @Override
    public double calculateFee(Ticket ticket) {
        double baseFee = base.calculateFee(ticket);
        LocalDateTime checkoutTime = ticket.getExitTime() != null ? ticket.getExitTime() : LocalDateTime.now();
        int hour = checkoutTime.getHour();
        // peakStartHour <= peakEndHour is a same-day window (e.g. 9-17): peak iff hour is between
        // them. peakStartHour > peakEndHour is an overnight window that wraps past midnight (e.g.
        // 22-6): peak iff hour is at/after start OR before end, since no single hour value can
        // satisfy "between 22 and 6" under the same-day comparison.
        boolean isPeak = peakStartHour <= peakEndHour
                ? (hour >= peakStartHour && hour < peakEndHour)
                : (hour >= peakStartHour || hour < peakEndHour);
        return isPeak ? baseFee * multiplier : baseFee;
    }
}