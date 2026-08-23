package com.smartparking.service;

import com.smartparking.model.Payment;
import com.smartparking.payment.CashPayment;
import com.smartparking.repository.InMemoryPaymentRepository;
import com.smartparking.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * EVChargingService's real tick constants (1s / 5%) make a full 100% charge a 20s test, so these
 * cover the fast edge cases instead: double-start, stop-without-start, and a short real charge
 * to confirm the billed fee tracks whatever chargePercent actually reached before stopCharging
 * joined the session thread.
 */
class EVChargingServiceTest {

    private EVChargingService evChargingService;

    @BeforeEach
    void setUp() {
        PaymentRepository paymentRepository = new InMemoryPaymentRepository();
        PaymentService paymentService = new PaymentService(paymentRepository);
        evChargingService = new EVChargingService(paymentService);
    }

    @Test
    void startingChargingTwiceOnSameSlotThrows() {
        evChargingService.startCharging(1L, 100L);
        try {
            assertThrows(IllegalStateException.class, () -> evChargingService.startCharging(2L, 100L));
        } finally {
            evChargingService.stopCharging(100L, new CashPayment());
        }
    }

    @Test
    void stopChargingWithoutActiveSessionThrows() {
        assertThrows(IllegalArgumentException.class, () -> evChargingService.stopCharging(999L, new CashPayment()));
    }

    @Test
    void isChargingAndGetChargePercentReflectSessionLifecycle() {
        assertFalse(evChargingService.isCharging(100L));
        assertEquals(0, evChargingService.getChargePercent(100L));

        evChargingService.startCharging(1L, 100L);
        assertTrue(evChargingService.isCharging(100L));

        evChargingService.stopCharging(100L, new CashPayment());
        assertFalse(evChargingService.isCharging(100L), "stopCharging should remove the session, not just mark it stopped");
    }

    @Test
    void stopChargingBillsFeeProportionalToChargeDelivered() throws InterruptedException {
        evChargingService.startCharging(1L, 100L);
        Thread.sleep(1100); // let at least one 1s/5% tick land

        Payment payment = evChargingService.stopCharging(100L, new CashPayment());

        assertFalse(evChargingService.isCharging(100L), "session should be gone after stopCharging");
        // Each tick delivers exactly 5% at 0.10/percent, i.e. 0.50 per tick -- the fee must land
        // on that grid, never a fraction of a tick (which would mean it billed a mid-tick read).
        double ticksBilled = payment.getAmount() / 0.50;
        assertTrue(payment.getAmount() > 0, "at least one tick should have delivered some charge to bill for");
        assertEquals(Math.round(ticksBilled), ticksBilled, 1e-9,
                "fee should correspond to a whole number of completed ticks");
    }

    @Test
    void independentSlotsChargeIndependently() {
        evChargingService.startCharging(1L, 100L);
        evChargingService.startCharging(2L, 200L);

        assertTrue(evChargingService.isCharging(100L));
        assertTrue(evChargingService.isCharging(200L));

        evChargingService.stopCharging(100L, new CashPayment());
        assertFalse(evChargingService.isCharging(100L));
        assertTrue(evChargingService.isCharging(200L), "stopping one slot's session must not affect another slot's session");

        evChargingService.stopCharging(200L, new CashPayment());
    }
}
