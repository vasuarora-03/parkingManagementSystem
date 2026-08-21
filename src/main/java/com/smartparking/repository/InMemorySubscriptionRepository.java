package com.smartparking.repository;

import com.smartparking.model.Subscription;

import java.util.Optional;

public class InMemorySubscriptionRepository extends AbstractInMemoryRepository<Subscription> implements SubscriptionRepository {
    public InMemorySubscriptionRepository() {
        super(Subscription::getId, Subscription::setId);
    }

    @Override
    public Optional<Subscription> findByVehicleId(Long vehicleId) {
        return store.values().stream()
                .filter(s -> s.getVehicleId().equals(vehicleId))
                .findFirst();
    }
}