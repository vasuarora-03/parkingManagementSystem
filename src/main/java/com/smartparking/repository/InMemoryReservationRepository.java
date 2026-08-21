package com.smartparking.repository;

import com.smartparking.model.Reservation;
import com.smartparking.model.ReservationStatus;

import java.util.Optional;

public class InMemoryReservationRepository extends AbstractInMemoryRepository<Reservation> implements ReservationRepository {
    public InMemoryReservationRepository() {
        super(Reservation::getId, Reservation::setId);
    }

    @Override
    public Optional<Reservation> findActiveByVehicleId(Long vehicleId) {
        return store.values().stream()
                .filter(r -> r.getVehicleId().equals(vehicleId)
                        && (r.getStatus() == ReservationStatus.PENDING || r.getStatus() == ReservationStatus.CONFIRMED))
                .findFirst();
    }
}