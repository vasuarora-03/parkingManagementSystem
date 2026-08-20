package com.smartparking.repository;

import com.smartparking.model.Vehicle;

public class InMemoryVehicleRepository extends AbstractInMemoryRepository<Vehicle> implements VehicleRepository {
    public InMemoryVehicleRepository() {
        super(Vehicle::getId, Vehicle::setId);
    }
}