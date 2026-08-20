package com.smartparking.model;

/**
 * A registered vehicle. Pure data — no behavior lives here, which is why it's a plain
 * class instead of a Car/Bike/Truck subclass hierarchy (they don't behave differently,
 * they're only labeled differently via VehicleType).
 *
 * id is nullable and left unset until a repository assigns it on save() — this mirrors
 * how @GeneratedValue works once this becomes a JPA @Entity: the object exists in memory
 * before storage decides its identity.
 */
public class Vehicle {
    private Long id;
    private String licensePlate;
    private String ownerName;
    private String ownerContact;
    private VehicleType type;
    private boolean electric;

    public Vehicle(String licensePlate, String ownerName, String ownerContact, VehicleType type, boolean electric) {
        this.licensePlate = licensePlate;
        this.ownerName = ownerName;
        this.ownerContact = ownerContact;
        this.type = type;
        this.electric = electric;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLicensePlate() {
        return licensePlate;
    }

    public void setLicensePlate(String licensePlate) {
        this.licensePlate = licensePlate;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public String getOwnerContact() {
        return ownerContact;
    }

    public void setOwnerContact(String ownerContact) {
        this.ownerContact = ownerContact;
    }

    public VehicleType getType() {
        return type;
    }

    public void setType(VehicleType type) {
        this.type = type;
    }

    public boolean isElectric() {
        return electric;
    }

    public void setElectric(boolean electric) {
        this.electric = electric;
    }

    @Override
    public String toString() {
        return "Vehicle{id=" + id + ", plate='" + licensePlate + "', owner='" + ownerName
                + "', type=" + type + ", electric=" + electric + "}";
    }
}