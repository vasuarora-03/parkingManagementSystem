package com.smartparking.pricing;

import com.smartparking.model.Ticket;

/** rate x duration, rounded up to the next full day and billed for at least one day. */
public class DailyPricing implements PricingStrategy {
    private final double ratePerDay;

    public DailyPricing(double ratePerDay) {
        this.ratePerDay = ratePerDay;
    }

    @Override
    public double calculateFee(Ticket ticket) {
        double days = Math.ceil(minutesParked(ticket) / (60.0 * 24));
        return Math.max(days, 1) * ratePerDay;
    }
}