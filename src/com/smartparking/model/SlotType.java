package com.smartparking.model;

/**
 * Category of parking slot. Mirrors VehicleType's values but is kept as a separate enum
 * on purpose: a slot's type and a vehicle's type are different concepts that only happen
 * to share the same set of labels today (a slot type could later gain values a vehicle
 * type never needs, e.g. COMPACT or OVERSIZED).
 */
public enum SlotType {
    CAR, BIKE, TRUCK
}