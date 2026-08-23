package com.smartparking.pricing;

import com.smartparking.model.Ticket;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Covers the peakStartHour > peakEndHour overnight wraparound (e.g. 22-6) alongside the
 * ordinary same-day window (e.g. 9-17), including the inclusive-start/exclusive-end boundary
 * hours on both, since that boundary is exactly where an off-by-one in the wraparound fix
 * would show up first.
 */
class SurgePricingTest {

    private static final double BASE_RATE = 10.0;
    private static final double MULTIPLIER = 2.0;

    /** Always a fixed 1-hour park, so the base fee is constant -- only exitTime's hour-of-day (and
     *  therefore whether SurgePricing treats it as peak) varies between test cases. */
    private Ticket ticketAt(int hour) {
        LocalDateTime exit = LocalDateTime.of(2026, 1, 1, 12, 0).withHour(hour);
        Ticket ticket = new Ticket(1L, 1L, exit.minusHours(1));
        ticket.setExitTime(exit);
        return ticket;
    }

    @Test
    void sameDayWindowAppliesSurgeInsideAndNotOutside() {
        SurgePricing surge = new SurgePricing(new HourlyPricing(BASE_RATE), MULTIPLIER, 9, 17);

        assertEquals(BASE_RATE * MULTIPLIER, surge.calculateFee(ticketAt(9)), "start hour is inclusive");
        assertEquals(BASE_RATE * MULTIPLIER, surge.calculateFee(ticketAt(12)), "midday should be peak");
        assertEquals(BASE_RATE, surge.calculateFee(ticketAt(17)), "end hour is exclusive");
        assertEquals(BASE_RATE, surge.calculateFee(ticketAt(8)), "just before the window is off-peak");
        assertEquals(BASE_RATE, surge.calculateFee(ticketAt(20)), "well after the window is off-peak");
    }

    @Test
    void overnightWraparoundWindowAppliesSurgeAcrossMidnight() {
        SurgePricing surge = new SurgePricing(new HourlyPricing(BASE_RATE), MULTIPLIER, 22, 6);

        assertEquals(BASE_RATE * MULTIPLIER, surge.calculateFee(ticketAt(22)), "start hour is inclusive");
        assertEquals(BASE_RATE * MULTIPLIER, surge.calculateFee(ticketAt(23)), "late night should be peak");
        assertEquals(BASE_RATE * MULTIPLIER, surge.calculateFee(ticketAt(0)), "midnight should be peak");
        assertEquals(BASE_RATE * MULTIPLIER, surge.calculateFee(ticketAt(3)), "early morning should be peak");
        assertEquals(BASE_RATE, surge.calculateFee(ticketAt(6)), "end hour is exclusive");
        assertEquals(BASE_RATE, surge.calculateFee(ticketAt(12)), "midday is outside an overnight window");
        assertEquals(BASE_RATE, surge.calculateFee(ticketAt(21)), "just before an overnight window is off-peak");
    }

    @Test
    void nullExitTimeFallsBackToNowWithoutThrowing() {
        SurgePricing surge = new SurgePricing(new HourlyPricing(BASE_RATE), MULTIPLIER, 0, 24);
        Ticket ticket = new Ticket(1L, 1L, LocalDateTime.now().minusHours(1));
        // exitTime intentionally left null -- calculateFee must not NPE and must still price something.
        double fee = surge.calculateFee(ticket);
        assertEquals(BASE_RATE * MULTIPLIER, fee, "0-24 window covers every hour, including 'now'");
    }
}
