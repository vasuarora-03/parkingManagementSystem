package com.smartparking.model;

/** One physical slot in the lot. status is what the concurrency fix in ParkingSlotService protects. */
public class ParkingSlot {
    private Long id;
    private String slotNumber;
    private int floor;
    private SlotType type;
    private SlotStatus status;
    private boolean hasEvCharger;

    public ParkingSlot(String slotNumber, int floor, SlotType type, boolean hasEvCharger) {
        this.slotNumber = slotNumber;
        this.floor = floor;
        this.type = type;
        this.hasEvCharger = hasEvCharger;
        this.status = SlotStatus.AVAILABLE;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSlotNumber() {
        return slotNumber;
    }

    public void setSlotNumber(String slotNumber) {
        this.slotNumber = slotNumber;
    }

    public int getFloor() {
        return floor;
    }

    public void setFloor(int floor) {
        this.floor = floor;
    }

    public SlotType getType() {
        return type;
    }

    public void setType(SlotType type) {
        this.type = type;
    }

    public SlotStatus getStatus() {
        return status;
    }

    public void setStatus(SlotStatus status) {
        this.status = status;
    }

    public boolean isHasEvCharger() {
        return hasEvCharger;
    }

    public void setHasEvCharger(boolean hasEvCharger) {
        this.hasEvCharger = hasEvCharger;
    }

    @Override
    public String toString() {
        return "ParkingSlot{id=" + id + ", number='" + slotNumber + "', floor=" + floor
                + ", type=" + type + ", status=" + status + ", evCharger=" + hasEvCharger + "}";
    }
}