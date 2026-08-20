package com.smartparking.pricing;

import com.smartparking.model.Ticket;

/** rate x duration, rounded up to the next full hour and billed for at least one hour — how most real lots do it. */
public class HourlyPricing implements PricingStrategy {
    private final double ratePerHour;

    public HourlyPricing(double ratePerHour) {
        this.ratePerHour = ratePerHour;
    }

    @Override
    public double calculateFee(Ticket ticket) {
        double hours = Math.ceil(minutesParked(ticket) / 60.0);
        return Math.max(hours, 1) * ratePerHour;
    }
}