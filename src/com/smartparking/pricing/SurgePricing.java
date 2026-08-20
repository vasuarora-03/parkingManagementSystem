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
        boolean isPeak = hour >= peakStartHour && hour < peakEndHour;
        return isPeak ? baseFee * multiplier : baseFee;
    }
}