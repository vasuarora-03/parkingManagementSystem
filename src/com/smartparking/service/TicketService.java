package com.smartparking.service;

import com.smartparking.exception.DuplicateBookingException;
import com.smartparking.model.ParkingSlot;
import com.smartparking.model.Payment;
import com.smartparking.model.Ticket;
import com.smartparking.model.User;
import com.smartparking.model.Vehicle;
import com.smartparking.notification.NotificationChannel;
import com.smartparking.payment.PaymentMethod;
import com.smartparking.pricing.PricingStrategy;
import com.smartparking.repository.TicketRepository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * The main "business logic" class: opens a Ticket at check-in, and at check-out orchestrates
 * fee calculation, slot release, payment, and notification — the full flow the spec describes.
 */
public class TicketService {

    private final TicketRepository ticketRepository;
    private final ParkingSlotService parkingSlotService;
    private final PaymentService paymentService;
    private final NotificationChannel notificationChannel;
    private final EVChargingService evChargingService;

    // Guards the "does this vehicle already have an open ticket" check together with the
    // save that follows it — same check-then-act race as ReservationService.bookingLock,
    // just for walk-in/confirmed check-ins instead of reservations.
    private final Object checkInLock = new Object();

    public TicketService(TicketRepository ticketRepository, ParkingSlotService parkingSlotService,
                          PaymentService paymentService, NotificationChannel notificationChannel,
                          EVChargingService evChargingService) {
        this.ticketRepository = ticketRepository;
        this.parkingSlotService = parkingSlotService;
        this.paymentService = paymentService;
        this.notificationChannel = notificationChannel;
        this.evChargingService = evChargingService;
    }

    public Optional<Ticket> getTicket(Long id) {
        return ticketRepository.findById(id);
    }

    public Ticket checkIn(Vehicle vehicle, ParkingSlot slot) {
        synchronized (checkInLock) {
            ticketRepository.findOpenByVehicleId(vehicle.getId()).ifPresent(existing -> {
                throw new DuplicateBookingException(
                        "Vehicle " + vehicle.getLicensePlate() + " already has an open ticket (id=" + existing.getId() + ")");
            });
            Ticket ticket = new Ticket(vehicle.getId(), slot.getId(), LocalDateTime.now());
            return ticketRepository.save(ticket);
        }
    }

    public CheckoutReceipt checkOut(Long ticketId, PricingStrategy pricingStrategy, PaymentMethod paymentMethod, User user) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("No ticket with id " + ticketId));
        if (!ticket.isOpen()) {
            throw new IllegalStateException("Ticket " + ticketId + " is already closed");
        }
        if (evChargingService.isCharging(ticket.getSlotId())) {
            throw new IllegalStateException(
                    "Slot " + ticket.getSlotId() + " is still charging — stop EV charging before checking out");
        }

        // exitTime must be set BEFORE calculateFee runs: pricing strategies measure duration up
        // to ticket.getExitTime(), so the fee they compute matches exactly what gets billed rather
        // than drifting from a second, slightly-later "now" read during fee calculation.
        ticket.setExitTime(LocalDateTime.now());
        double fee = pricingStrategy.calculateFee(ticket);
        ticket.setTotalFee(fee);
        ticketRepository.save(ticket);

        // The slot is released before payment is attempted on purpose: physically, the vehicle
        // has already left, so the space needs to become available to the next customer
        // regardless of whether billing succeeds. A failed payment is a billing problem to
        // resolve afterward, not a reason to strand the slot as occupied.
        parkingSlotService.releaseSlot(ticket.getSlotId());

        // If paymentService.pay(...) throws PaymentFailedException, it propagates straight out
        // of checkOut to the caller (the CLI boundary, per §9) — the ticket is already closed
        // and the slot already freed by this point, only the payment itself is left unresolved.
        // Because the throw happens before the line below, a failed payment never triggers a
        // "thanks, paid!" notification.
        Payment payment = paymentService.pay(ticket.getId(), fee, paymentMethod);

        notificationChannel.notify(user, String.format(
                "Checkout complete: parked %s to %s, fee %.2f paid via %s.",
                ticket.getEntryTime(), ticket.getExitTime(), ticket.getTotalFee(), payment.getMethod()));

        return new CheckoutReceipt(ticket, payment);
    }
}