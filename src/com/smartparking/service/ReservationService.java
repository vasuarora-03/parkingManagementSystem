package com.smartparking.service;

import com.smartparking.exception.DuplicateBookingException;
import com.smartparking.model.ParkingSlot;
import com.smartparking.model.Reservation;
import com.smartparking.model.ReservationStatus;
import com.smartparking.model.SlotType;
import com.smartparking.model.Vehicle;
import com.smartparking.repository.ReservationRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Advance-booking workflow. Sits on top of ParkingSlotService rather than touching
 * ParkingSlotRepository directly — the slot-claiming atomicity from §8 lives in exactly
 * one place (ParkingSlotService), and this class just orchestrates the Reservation record
 * around it.
 */
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final ParkingSlotService parkingSlotService;

    public ReservationService(ReservationRepository reservationRepository, ParkingSlotService parkingSlotService) {
        this.reservationRepository = reservationRepository;
        this.parkingSlotService = parkingSlotService;
    }

    /** Holds a slot of the given type for the vehicle for holdMinutes, before the vehicle has arrived. */
    public Reservation reserve(Vehicle vehicle, SlotType slotType, int holdMinutes) {
        reservationRepository.findActiveByVehicleId(vehicle.getId()).ifPresent(existing -> {
            throw new DuplicateBookingException(
                    "Vehicle " + vehicle.getLicensePlate() + " already has an active reservation (id=" + existing.getId() + ")");
        });

        // Atomic claim happens inside ParkingSlotService — by the time this returns, the slot is ours alone.
        ParkingSlot slot = parkingSlotService.reserveSlot(vehicle, slotType);

        LocalDateTime now = LocalDateTime.now();
        Reservation reservation = new Reservation(vehicle.getId(), slot.getId(), now, now.plusMinutes(holdMinutes));
        return reservationRepository.save(reservation);
    }

    /** Called when the vehicle actually arrives: turns the hold into a real occupancy. */
    public Reservation confirm(Long reservationId) {
        Reservation reservation = getPending(reservationId);

        if (LocalDateTime.now().isAfter(reservation.getExpiresAt())) {
            expire(reservation);
            throw new IllegalStateException("Reservation " + reservationId + " expired at " + reservation.getExpiresAt());
        }

        reservation.setStatus(ReservationStatus.CONFIRMED);
        parkingSlotService.occupySlot(reservation.getSlotId());
        return reservationRepository.save(reservation);
    }

    /** Customer-initiated cancellation of a hold that hasn't been confirmed yet. */
    public void cancel(Long reservationId) {
        Reservation reservation = getPending(reservationId);
        reservation.setStatus(ReservationStatus.CANCELLED);
        reservationRepository.save(reservation);
        parkingSlotService.releaseSlot(reservation.getSlotId());
    }

    /** Used by ReservationExpiryMonitor (§8 background thread) to sweep past-due holds. */
    public void expire(Reservation reservation) {
        reservation.setStatus(ReservationStatus.EXPIRED);
        reservationRepository.save(reservation);
        parkingSlotService.releaseSlot(reservation.getSlotId());
    }

    public List<Reservation> findAllPending() {
        return reservationRepository.findAll().stream()
                .filter(r -> r.getStatus() == ReservationStatus.PENDING)
                .collect(Collectors.toList());
    }

    private Reservation getPending(Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("No reservation with id " + reservationId));
        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new IllegalStateException(
                    "Reservation " + reservationId + " is not pending (status=" + reservation.getStatus() + ")");
        }
        return reservation;
    }
}