package com.smartparking.service;

import com.smartparking.exception.SlotNotAvailableException;
import com.smartparking.model.ParkingSlot;
import com.smartparking.model.SlotType;
import com.smartparking.model.Vehicle;
import com.smartparking.model.VehicleType;
import com.smartparking.repository.InMemoryParkingSlotRepository;
import com.smartparking.repository.ParkingSlotRepository;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Reproduces the double-booking race described in ParkingSlotService's allocationLock
 * comment: many threads racing for one free slot. Before the synchronized fix, this test
 * would flake with more than one thread believing it won the slot.
 */
class ParkingSlotServiceTest {

    @Test
    void onlyOneThreadWinsWhenManyThreadsRaceForOneSlot() throws InterruptedException {
        ParkingSlotRepository repository = new InMemoryParkingSlotRepository();
        ParkingSlotService service = new ParkingSlotService(repository);
        service.registerSlot(new ParkingSlot("C-1", 1, SlotType.CAR, false));

        int threadCount = 50;
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger successes = new AtomicInteger();
        AtomicInteger rejections = new AtomicInteger();

        for (int i = 0; i < threadCount; i++) {
            Vehicle vehicle = new Vehicle("PLATE-" + i, "Owner " + i, "000", VehicleType.CAR, false);
            pool.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    service.allocateSlot(vehicle);
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
        assertEquals(true, pool.awaitTermination(10, TimeUnit.SECONDS));

        assertEquals(1, successes.get(), "exactly one thread should have claimed the only slot");
        assertEquals(threadCount - 1, rejections.get(), "every other thread should be rejected, not double-booked");
    }
}
