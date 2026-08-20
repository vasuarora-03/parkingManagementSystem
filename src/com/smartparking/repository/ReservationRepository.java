package com.smartparking.repository;

import com.smartparking.model.Reservation;

import java.util.Optional;

public interface ReservationRepository extends Repository<Reservation, Long> {
    /** The vehicle's currently PENDING or CONFIRMED reservation, if any — used to reject a second booking for the same vehicle. */
    Optional<Reservation> findActiveByVehicleId(Long vehicleId);
}