package com.smartparking.service;

import com.smartparking.exception.SlotNotAvailableException;
import com.smartparking.model.ParkingSlot;
import com.smartparking.model.SlotStatus;
import com.smartparking.model.SlotType;
import com.smartparking.model.Vehicle;
import com.smartparking.model.VehicleType;
import com.smartparking.repository.ParkingSlotRepository;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Owns every transition a ParkingSlot's status goes through. Nothing outside this class
 * (not even other services) should flip a slot's status directly — that's what makes it
 * possible to guarantee the invariant below in exactly one place.
 */
public class ParkingSlotService {

    private final ParkingSlotRepository parkingSlotRepository;

    /**
     * THE §8 CONCURRENCY FIX.
     *
     * The bug: "find an available slot, then mark it occupied" is two separate operations —
     * a read (findAvailableByType) and a write (save). Say there's exactly one free CAR slot
     * left, and two customers hit "park" at nearly the same instant on two different threads:
     *
     *   Thread A: findAvailableByType(CAR) -> sees slot #7 is AVAILABLE
     *   Thread B: findAvailableByType(CAR) -> ALSO sees slot #7 is AVAILABLE (A hasn't written yet)
     *   Thread A: save(slot #7 with status=OCCUPIED)
     *   Thread B: save(slot #7 with status=OCCUPIED)   <- B just "stole" a slot A already gave out
     *
     * Both threads now believe they own slot #7. That's the double-booking bug. Note that
     * ConcurrentHashMap being thread-safe does NOT save us here — it only guarantees each
     * individual get()/put() is safe, not that a read-then-write pair spanning two calls is
     * atomic as a whole. The map's internal safety is a different property from the business
     * invariant we need ("only one thread can hold a given slot's fate at a time").
     *
     * The fix: wrap the whole read-then-write sequence in a synchronized block on one lock
     * object shared by every slot-claiming method. This turns "find + claim" into a single
     * critical section — the second thread can't even start its find until the first thread's
     * write has completed, so it will correctly see the slot as no-longer-available. This is
     * intentionally a private, dedicated Object (not `synchronized` on `this`) so nothing
     * outside this class can accidentally lock on — or interfere with — this monitor.
     *
     * A real database solves the exact same problem differently but for the same reason: a
     * UNIQUE constraint on (slot_id) with status='OCCUPIED', or an optimistic-lock @Version
     * column that makes a conflicting concurrent UPDATE fail and retry. Same race, different
     * mechanism once storage isn't a plain in-memory map.
     */
    private final Object allocationLock = new Object();

    public ParkingSlotService(ParkingSlotRepository parkingSlotRepository) {
        this.parkingSlotRepository = parkingSlotRepository;
    }

    /** Adds a slot to the lot's inventory. Used at startup to seed the lot — still goes through the service, never the repository directly. */
    public ParkingSlot registerSlot(ParkingSlot slot) {
        return parkingSlotRepository.save(slot);
    }

    public Optional<ParkingSlot> getSlot(Long slotId) {
        return parkingSlotRepository.findById(slotId);
    }

    /** Walk-in path: no prior reservation, vehicle is here now — claim a slot and mark it OCCUPIED immediately. */
    public ParkingSlot allocateSlot(Vehicle vehicle) {
        return claimSlot(vehicle, toSlotType(vehicle.getType()), SlotStatus.OCCUPIED);
    }

    /** Advance-booking path: vehicle isn't here yet — claim a slot but mark it RESERVED, not OCCUPIED. */
    public ParkingSlot reserveSlot(Vehicle vehicle, SlotType slotType) {
        return claimSlot(vehicle, slotType, SlotStatus.RESERVED);
    }

    /** Called when a held reservation's vehicle actually arrives — the slot is already claimed, this just flips RESERVED -> OCCUPIED. */
    public ParkingSlot occupySlot(Long slotId) {
        synchronized (allocationLock) {
            ParkingSlot slot = parkingSlotRepository.findById(slotId)
                    .orElseThrow(() -> new IllegalArgumentException("No slot with id " + slotId));
            slot.setStatus(SlotStatus.OCCUPIED);
            return parkingSlotRepository.save(slot);
        }
    }

    /** Frees a slot back to AVAILABLE — used on checkout, on cancellation, and on reservation expiry. */
    public void releaseSlot(Long slotId) {
        synchronized (allocationLock) {
            ParkingSlot slot = parkingSlotRepository.findById(slotId)
                    .orElseThrow(() -> new IllegalArgumentException("No slot with id " + slotId));
            slot.setStatus(SlotStatus.AVAILABLE);
            parkingSlotRepository.save(slot);
        }
    }

    /** Snapshot of how many AVAILABLE slots exist per type, for the CLI's "live availability" view. */
    public Map<SlotType, Long> getLiveAvailability() {
        Map<SlotType, Long> counts = new EnumMap<>(SlotType.class);
        for (SlotType type : SlotType.values()) {
            counts.put(type, (long) parkingSlotRepository.findAvailableByType(type).size());
        }
        return counts;
    }

    private ParkingSlot claimSlot(Vehicle vehicle, SlotType slotType, SlotStatus targetStatus) {
        synchronized (allocationLock) {
            List<ParkingSlot> available = parkingSlotRepository.findAvailableByType(slotType);
            if (available.isEmpty()) {
                throw new SlotNotAvailableException(
                        "No available " + slotType + " slot for vehicle " + vehicle.getLicensePlate());
            }
            // EVs prefer a slot with a charger when one happens to be free, but any free slot of the right type will do.
            ParkingSlot chosen = vehicle.isElectric()
                    ? available.stream().filter(ParkingSlot::isHasEvCharger).findFirst().orElse(available.get(0))
                    : available.get(0);

            chosen.setStatus(targetStatus);
            return parkingSlotRepository.save(chosen);
        }
    }

    private SlotType toSlotType(VehicleType vehicleType) {
        return SlotType.valueOf(vehicleType.name());
    }
}