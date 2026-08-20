package com.smartparking.pricing;

import com.smartparking.model.Ticket;
import com.smartparking.service.SubscriptionService;

/**
 * Wraps a base strategy but overrides it with a small flat fee (or 0.0) whenever the vehicle
 * has an active subscription. Checked via SubscriptionService.isActive(vehicleId) — never by
 * reaching into SubscriptionRepository directly, so this respects the same "only talk to
 * services" boundary the rest of the app follows.
 */
public class SubscriptionPricing implements PricingStrategy {
    private final PricingStrategy base;
    private final SubscriptionService subscriptionService;
    private final double subscriberFlatFee; // e.g. 0.0 for fully waived, or a small remainder fee

    public SubscriptionPricing(PricingStrategy base, SubscriptionService subscriptionService, double subscriberFlatFee) {
        this.base = base;
        this.subscriptionService = subscriptionService;
        this.subscriberFlatFee = subscriberFlatFee;
    }

    @Override
    public double calculateFee(Ticket ticket) {
        if (subscriptionService.isActive(ticket.getVehicleId())) {
            return subscriberFlatFee;
        }
        return base.calculateFee(ticket);
    }
}