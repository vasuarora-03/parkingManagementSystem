package com.smartparking.service;

import com.smartparking.model.Subscription;
import com.smartparking.model.SubscriptionPlan;
import com.smartparking.repository.SubscriptionRepository;

import java.time.LocalDateTime;

/** Enrollment/renewal for monthly/annual plans. isActive(vehicleId) is what SubscriptionPricing calls into. */
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;

    public SubscriptionService(SubscriptionRepository subscriptionRepository) {
        this.subscriptionRepository = subscriptionRepository;
    }

    public Subscription enroll(Long userId, Long vehicleId, SubscriptionPlan plan) {
        LocalDateTime now = LocalDateTime.now();
        Subscription subscription = new Subscription(userId, vehicleId, plan, now, validTo(plan, now));
        return subscriptionRepository.save(subscription);
    }

    public Subscription renew(Long vehicleId) {
        Subscription subscription = subscriptionRepository.findByVehicleId(vehicleId)
                .orElseThrow(() -> new IllegalArgumentException("No subscription found for vehicle " + vehicleId));
        LocalDateTime now = LocalDateTime.now();
        subscription.setValidFrom(now);
        subscription.setValidTo(validTo(subscription.getPlan(), now));
        subscription.setActive(true);
        return subscriptionRepository.save(subscription);
    }

    /** True only if a subscription exists, is flagged active, and hasn't passed its validTo date. */
    public boolean isActive(Long vehicleId) {
        return subscriptionRepository.findByVehicleId(vehicleId)
                .filter(Subscription::isActive)
                .filter(s -> LocalDateTime.now().isBefore(s.getValidTo()))
                .isPresent();
    }

    private LocalDateTime validTo(SubscriptionPlan plan, LocalDateTime from) {
        return plan == SubscriptionPlan.MONTHLY ? from.plusMonths(1) : from.plusYears(1);
    }
}