package com.smartparking.model;

import java.time.LocalDateTime;

/** An advance booking hold on a slot — exists before the vehicle physically arrives. */
public class Reservation {
    private Long id;
    private Long vehicleId;
    private Long slotId;
    private LocalDateTime reservedFrom;
    private LocalDateTime expiresAt;
    private ReservationStatus status;

    public Reservation(Long vehicleId, Long slotId, LocalDateTime reservedFrom, LocalDateTime expiresAt) {
        this.vehicleId = vehicleId;
        this.slotId = slotId;
        this.reservedFrom = reservedFrom;
        this.expiresAt = expiresAt;
        this.status = ReservationStatus.PENDING;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(Long vehicleId) {
        this.vehicleId = vehicleId;
    }

    public Long getSlotId() {
        return slotId;
    }

    public void setSlotId(Long slotId) {
        this.slotId = slotId;
    }

    public LocalDateTime getReservedFrom() {
        return reservedFrom;
    }

    public void setReservedFrom(LocalDateTime reservedFrom) {
        this.reservedFrom = reservedFrom;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public void setStatus(ReservationStatus status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "Reservation{id=" + id + ", vehicleId=" + vehicleId + ", slotId=" + slotId
                + ", status=" + status + ", expiresAt=" + expiresAt + "}";
    }
}