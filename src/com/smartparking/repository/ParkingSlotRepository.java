package com.smartparking.repository;

import com.smartparking.model.ParkingSlot;
import com.smartparking.model.SlotType;

import java.util.List;

public interface ParkingSlotRepository extends Repository<ParkingSlot, Long> {
    /** All slots of the given type currently AVAILABLE. Read-only — allocation atomicity is ParkingSlotService's job, not the repository's (see §8). */
    List<ParkingSlot> findAvailableByType(SlotType type);
}