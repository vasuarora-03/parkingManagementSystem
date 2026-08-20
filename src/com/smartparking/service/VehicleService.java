package com.smartparking.service;

import com.smartparking.model.Vehicle;
import com.smartparking.model.VehicleType;
import com.smartparking.repository.VehicleRepository;

import java.util.Optional;

/** Not named in §6 — same gap as UserService: Main can't call VehicleRepository directly. */
public class VehicleService {

    private final VehicleRepository vehicleRepository;

    public VehicleService(VehicleRepository vehicleRepository) {
        this.vehicleRepository = vehicleRepository;
    }

    public Vehicle register(String licensePlate, String ownerName, String ownerContact, VehicleType type, boolean electric) {
        return vehicleRepository.save(new Vehicle(licensePlate, ownerName, ownerContact, type, electric));
    }

    public Optional<Vehicle> getVehicle(Long id) {
        return vehicleRepository.findById(id);
    }
}