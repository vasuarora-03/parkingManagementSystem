package com.smartparking.repository;

import com.smartparking.model.Ticket;

import java.util.Optional;

public class InMemoryTicketRepository extends AbstractInMemoryRepository<Ticket> implements TicketRepository {
    public InMemoryTicketRepository() {
        super(Ticket::getId, Ticket::setId);
    }

    @Override
    public Optional<Ticket> findOpenByVehicleId(Long vehicleId) {
        return store.values().stream()
                .filter(t -> t.getVehicleId().equals(vehicleId) && t.isOpen())
                .findFirst();
    }
}