package com.smartparking.repository;

import com.smartparking.model.ParkingSlot;
import com.smartparking.model.SlotStatus;
import com.smartparking.model.SlotType;

import java.util.List;
import java.util.stream.Collectors;

public class InMemoryParkingSlotRepository extends AbstractInMemoryRepository<ParkingSlot> implements ParkingSlotRepository {
    public InMemoryParkingSlotRepository() {
        super(ParkingSlot::getId, ParkingSlot::setId);
    }

    @Override
    public List<ParkingSlot> findAvailableByType(SlotType type) {
        return store.values().stream()
                .filter(slot -> slot.getType() == type && slot.getStatus() == SlotStatus.AVAILABLE)
                .collect(Collectors.toList());
    }
}