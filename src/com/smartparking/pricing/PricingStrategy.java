package com.smartparking.pricing;

import com.smartparking.model.Ticket;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Why this interface exists: fee calculation is the one place where "what kind of pricing"
 * is a genuinely different algorithm per case — linear by hour, flat by day, a peak-hour
 * multiplier wrapping another strategy, or waived entirely for subscribers. TicketService.checkOut
 * calls calculateFee(ticket) without knowing or caring which of these it got. Adding a new
 * pricing model later (e.g. WeekendPricing) means writing one new class here — zero existing
 * code touched. That's the Open/Closed Principle actually paying off, not just a buzzword.
 */
public interface PricingStrategy {
    double calculateFee(Ticket ticket);

    /** Shared by every strategy, not something that varies between them, so it lives here as a default method instead of being duplicated. */
    default long minutesParked(Ticket ticket) {
        LocalDateTime end = ticket.getExitTime() != null ? ticket.getExitTime() : LocalDateTime.now();
        return Duration.between(ticket.getEntryTime(), end).toMinutes();
    }
}