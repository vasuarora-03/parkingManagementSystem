package com.smartparking.model;

import java.time.LocalDateTime;

/** Monthly/annual plan tied to one vehicle. SubscriptionService.isActive() drives SubscriptionPricing. */
public class Subscription {
    private Long id;
    private Long userId;
    private Long vehicleId;
    private SubscriptionPlan plan;
    private LocalDateTime validFrom;
    private LocalDateTime validTo;
    private boolean active;

    public Subscription(Long userId, Long vehicleId, SubscriptionPlan plan, LocalDateTime validFrom, LocalDateTime validTo) {
        this.userId = userId;
        this.vehicleId = vehicleId;
        this.plan = plan;
        this.validFrom = validFrom;
        this.validTo = validTo;
        this.active = true;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(Long vehicleId) {
        this.vehicleId = vehicleId;
    }

    public SubscriptionPlan getPlan() {
        return plan;
    }

    public void setPlan(SubscriptionPlan plan) {
        this.plan = plan;
    }

    public LocalDateTime getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(LocalDateTime validFrom) {
        this.validFrom = validFrom;
    }

    public LocalDateTime getValidTo() {
        return validTo;
    }

    public void setValidTo(LocalDateTime validTo) {
        this.validTo = validTo;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public String toString() {
        return "Subscription{id=" + id + ", userId=" + userId + ", vehicleId=" + vehicleId
                + ", plan=" + plan + ", active=" + active + ", validTo=" + validTo + "}";
    }
}