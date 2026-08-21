package com.smartparking.repository;

import com.smartparking.model.Ticket;

import java.util.Optional;

public interface TicketRepository extends Repository<Ticket, Long> {
    /** The vehicle's currently open ticket (exitTime not yet set), if any — used to reject checking in twice. */
    Optional<Ticket> findOpenByVehicleId(Long vehicleId);
}