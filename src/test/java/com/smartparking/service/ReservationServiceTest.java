package com.smartparking.service;

import com.smartparking.exception.DuplicateBookingException;
import com.smartparking.exception.SlotNotAvailableException;
import com.smartparking.model.ParkingSlot;
import com.smartparking.model.Reservation;
import com.smartparking.model.ReservationStatus;
import com.smartparking.model.SlotStatus;
import com.smartparking.model.SlotType;
import com.smartparking.model.Vehicle;
import com.smartparking.model.VehicleType;
import com.smartparking.repository.InMemoryParkingSlotRepository;
import com.smartparking.repository.InMemoryReservationRepository;
import com.smartparking.repository.ParkingSlotRepository;
import com.smartparking.repository.ReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the bookingLock race described in ReservationService: two concurrent reserve() calls
 * for the same vehicle racing past the duplicate-active-reservation check, and the slot-claiming
 * race that ParkingSlotService.allocationLock already protects, exercised through this layer.
 */
class ReservationServiceTest {

    private ParkingSlotService slotService;
    private ReservationRepository reservationRepository;
    private ReservationService reservationService;

    @BeforeEach
    void setUp() {
        ParkingSlotRepository slotRepository = new InMemoryParkingSlotRepository();
        slotService = new ParkingSlotService(slotRepository);
        reservationRepository = new InMemoryReservationRepository();
        reservationService = new ReservationService(reservationRepository, slotService);
    }

    @Test
    void onlyOneReservationWinsWhenSameVehicleReservesConcurrently() throws InterruptedException {
        for (int i = 0; i < 20; i++) {
            slotService.registerSlot(new ParkingSlot("C-" + i, 1, SlotType.CAR, false));
        }
        Vehicle vehicle = new Vehicle("SAME-PLATE", "Owner", "000", VehicleType.CAR, false);
        vehicle.setId(1L);

        int threadCount = 20;
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger successes = new AtomicInteger();
        AtomicInteger duplicates = new AtomicInteger();

        for (int i = 0; i < threadCount; i++) {
            pool.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    reservationService.reserve(vehicle, SlotType.CAR, 30);
                    successes.incrementAndGet();
                } catch (DuplicateBookingException e) {
                    duplicates.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        ready.await();
        start.countDown();
        pool.shutdown();
        assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));

        assertEquals(1, successes.get(), "only the first reserve() for this vehicle should succeed");
        assertEquals(threadCount - 1, duplicates.get(), "every other concurrent call should see a duplicate booking, not a second reservation");
    }

    @Test
    void onlyOneVehicleWinsWhenManyVehiclesRaceForOneReservableSlot() throws InterruptedException {
        slotService.registerSlot(new ParkingSlot("C-1", 1, SlotType.CAR, false));

        int threadCount = 30;
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger successes = new AtomicInteger();
        AtomicInteger rejections = new AtomicInteger();

        for (int i = 0; i < threadCount; i++) {
            Vehicle vehicle = new Vehicle("PLATE-" + i, "Owner " + i, "000", VehicleType.CAR, false);
            vehicle.setId((long) (i + 1));
            pool.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    reservationService.reserve(vehicle, SlotType.CAR, 30);
                    successes.incrementAndGet();
                } catch (SlotNotAvailableException e) {
                    rejections.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        ready.await();
        start.countDown();
        pool.shutdown();
        assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));

        assertEquals(1, successes.get(), "exactly one vehicle should have claimed the only slot");
        assertEquals(threadCount - 1, rejections.get(), "every other vehicle should be rejected, not double-booked");
    }

    @Test
    void confirmAfterExpiryThrowsAndReleasesSlot() {
        ParkingSlot slot = slotService.registerSlot(new ParkingSlot("C-1", 1, SlotType.CAR, false));
        Vehicle vehicle = new Vehicle("PLATE-1", "Owner", "000", VehicleType.CAR, false);
        vehicle.setId(1L);

        LocalDateTime now = LocalDateTime.now();
        Reservation reservation = new Reservation(vehicle.getId(), slot.getId(), now.minusMinutes(30), now.minusMinutes(1));
        slotService.reserveSlot(vehicle, SlotType.CAR); // occupy the only slot so state matches an already-held reservation
        reservationRepository.save(reservation);

        assertThrows(IllegalStateException.class, () -> reservationService.confirm(reservation.getId()));
        assertEquals(ReservationStatus.EXPIRED, reservation.getStatus());
        assertEquals(SlotStatus.AVAILABLE, slotService.getSlot(slot.getId()).orElseThrow().getStatus(),
                "expiring on confirm() should release the slot back to the pool");
    }

    @Test
    void cancelReleasesSlotBackToAvailable() {
        slotService.registerSlot(new ParkingSlot("C-1", 1, SlotType.CAR, false));
        Vehicle vehicle = new Vehicle("PLATE-1", "Owner", "000", VehicleType.CAR, false);
        vehicle.setId(1L);

        Reservation reservation = reservationService.reserve(vehicle, SlotType.CAR, 30);
        reservationService.cancel(reservation.getId());

        assertEquals(ReservationStatus.CANCELLED, reservation.getStatus());
        assertEquals(SlotStatus.AVAILABLE, slotService.getSlot(reservation.getSlotId()).orElseThrow().getStatus());
    }

    @Test
    void confirmingAlreadyConfirmedReservationThrows() {
        slotService.registerSlot(new ParkingSlot("C-1", 1, SlotType.CAR, false));
        Vehicle vehicle = new Vehicle("PLATE-1", "Owner", "000", VehicleType.CAR, false);
        vehicle.setId(1L);

        Reservation reservation = reservationService.reserve(vehicle, SlotType.CAR, 30);
        reservationService.confirm(reservation.getId());

        assertThrows(IllegalStateException.class, () -> reservationService.confirm(reservation.getId()),
                "confirming a non-PENDING reservation a second time should be rejected, not silently re-applied");
    }
}
