package com.smartparking.model;

import java.time.LocalDateTime;

/**
 * The active/completed parking session — the core billing unit. Created at check-in
 * (entryTime set, exitTime/totalFee null), closed at check-out (exitTime + totalFee set).
 */
public class Ticket {
    private Long id;
    private Long vehicleId;
    private Long slotId;
    private LocalDateTime entryTime;
    private LocalDateTime exitTime;
    private Double totalFee;

    public Ticket(Long vehicleId, Long slotId, LocalDateTime entryTime) {
        this.vehicleId = vehicleId;
        this.slotId = slotId;
        this.entryTime = entryTime;
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

    public LocalDateTime getEntryTime() {
        return entryTime;
    }

    public void setEntryTime(LocalDateTime entryTime) {
        this.entryTime = entryTime;
    }

    public LocalDateTime getExitTime() {
        return exitTime;
    }

    public void setExitTime(LocalDateTime exitTime) {
        this.exitTime = exitTime;
    }

    public Double getTotalFee() {
        return totalFee;
    }

    public void setTotalFee(Double totalFee) {
        this.totalFee = totalFee;
    }

    public boolean isOpen() {
        return exitTime == null;
    }

    @Override
    public String toString() {
        return "Ticket{id=" + id + ", vehicleId=" + vehicleId + ", slotId=" + slotId
                + ", entryTime=" + entryTime + ", exitTime=" + exitTime + ", totalFee=" + totalFee + "}";
    }
}