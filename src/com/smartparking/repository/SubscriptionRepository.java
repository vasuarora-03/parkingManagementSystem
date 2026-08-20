package com.smartparking.repository;

import com.smartparking.model.Subscription;

import java.util.Optional;

public interface SubscriptionRepository extends Repository<Subscription, Long> {
    /** The vehicle's subscription record, if it has one — used by SubscriptionService.isActive(vehicleId). */
    Optional<Subscription> findByVehicleId(Long vehicleId);
}