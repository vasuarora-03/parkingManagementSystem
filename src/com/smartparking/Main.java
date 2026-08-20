package com.smartparking;

import com.smartparking.concurrency.ReservationExpiryMonitor;
import com.smartparking.exception.DuplicateBookingException;
import com.smartparking.exception.PaymentFailedException;
import com.smartparking.exception.SlotNotAvailableException;
import com.smartparking.model.ParkingSlot;
import com.smartparking.model.Payment;
import com.smartparking.model.Reservation;
import com.smartparking.model.SlotType;
import com.smartparking.model.Subscription;
import com.smartparking.model.SubscriptionPlan;
import com.smartparking.model.Ticket;
import com.smartparking.model.User;
import com.smartparking.model.UserRole;
import com.smartparking.model.Vehicle;
import com.smartparking.model.VehicleType;
import com.smartparking.notification.ConsoleNotification;
import com.smartparking.notification.NotificationChannel;
import com.smartparking.payment.CardPayment;
import com.smartparking.payment.CashPayment;
import com.smartparking.payment.PaymentMethod;
import com.smartparking.payment.UpiPayment;
import com.smartparking.payment.WalletPayment;
import com.smartparking.pricing.DailyPricing;
import com.smartparking.pricing.HourlyPricing;
import com.smartparking.pricing.PricingStrategy;
import com.smartparking.pricing.SubscriptionPricing;
import com.smartparking.pricing.SurgePricing;
import com.smartparking.repository.InMemoryParkingSlotRepository;
import com.smartparking.repository.InMemoryPaymentRepository;
import com.smartparking.repository.InMemoryReservationRepository;
import com.smartparking.repository.InMemorySubscriptionRepository;
import com.smartparking.repository.InMemoryTicketRepository;
import com.smartparking.repository.InMemoryUserRepository;
import com.smartparking.repository.InMemoryVehicleRepository;
import com.smartparking.repository.ParkingSlotRepository;
import com.smartparking.repository.PaymentRepository;
import com.smartparking.repository.ReservationRepository;
import com.smartparking.repository.SubscriptionRepository;
import com.smartparking.repository.TicketRepository;
import com.smartparking.repository.UserRepository;
import com.smartparking.repository.VehicleRepository;
import com.smartparking.service.CheckoutReceipt;
import com.smartparking.service.EVChargingService;
import com.smartparking.service.ParkingSlotService;
import com.smartparking.service.PaymentService;
import com.smartparking.service.ReservationService;
import com.smartparking.service.SubscriptionService;
import com.smartparking.service.TicketService;
import com.smartparking.service.UserService;
import com.smartparking.service.VehicleService;

import java.util.Map;
import java.util.Scanner;

/**
 * CLI entry point. Every menu action below calls into the service layer only — never a
 * repository directly — because each one is meant to become a REST endpoint later (§11):
 * this main() method's wiring block becomes Spring's dependency injection, and each
 * handleX(...) method's body becomes (most of) a @RestController method.
 */
public class Main {

    public static void main(String[] args) {
        // --- Wiring: repositories -> services. Manual `new X(...)` today; constructor
        // injection with @Service/@Component later, per §11 -- nothing else about these
        // objects' relationships changes.
        VehicleRepository vehicleRepository = new InMemoryVehicleRepository();
        ParkingSlotRepository parkingSlotRepository = new InMemoryParkingSlotRepository();
        ReservationRepository reservationRepository = new InMemoryReservationRepository();
        TicketRepository ticketRepository = new InMemoryTicketRepository();
        PaymentRepository paymentRepository = new InMemoryPaymentRepository();
        UserRepository userRepository = new InMemoryUserRepository();
        SubscriptionRepository subscriptionRepository = new InMemorySubscriptionRepository();

        VehicleService vehicleService = new VehicleService(vehicleRepository);
        UserService userService = new UserService(userRepository);
        ParkingSlotService parkingSlotService = new ParkingSlotService(parkingSlotRepository);
        ReservationService reservationService = new ReservationService(reservationRepository, parkingSlotService);
        SubscriptionService subscriptionService = new SubscriptionService(subscriptionRepository);
        PaymentService paymentService = new PaymentService(paymentRepository);
        NotificationChannel notificationChannel = new ConsoleNotification();
        TicketService ticketService = new TicketService(ticketRepository, parkingSlotService, paymentService, notificationChannel);
        EVChargingService evChargingService = new EVChargingService(paymentService);

        seedSlots(parkingSlotService);

        // Background sweep for expired reservations (§8) -- a Runnable handed to a daemon
        // Thread that Main owns, so it never blocks the JVM from exiting on its own.
        ReservationExpiryMonitor expiryMonitor = new ReservationExpiryMonitor(reservationService, 5000);
        Thread monitorThread = new Thread(expiryMonitor, "reservation-expiry-monitor");
        monitorThread.setDaemon(true);
        monitorThread.start();

        Scanner scanner = new Scanner(System.in);
        boolean running = true;
        while (running) {
            printMenu();
            int choice = readInt(scanner, "Choice: ");
            try {
                switch (choice) {
                    case 1:
                        handleRegisterVehicle(scanner, vehicleService);
                        break;
                    case 2:
                        handleRegisterUser(scanner, userService);
                        break;
                    case 3:
                        handleViewAvailability(parkingSlotService);
                        break;
                    case 4:
                        handleReserve(scanner, reservationService, vehicleService);
                        break;
                    case 5:
                        handleConfirmReservation(scanner, reservationService, vehicleService, parkingSlotService, ticketService);
                        break;
                    case 6:
                        handleCancelReservation(scanner, reservationService);
                        break;
                    case 7:
                        handleWalkInCheckIn(scanner, vehicleService, parkingSlotService, ticketService);
                        break;
                    case 8:
                        handleStartCharging(scanner, ticketService, vehicleService, evChargingService);
                        break;
                    case 9:
                        handleStopCharging(scanner, evChargingService);
                        break;
                    case 10:
                        handleCheckOut(scanner, ticketService, subscriptionService, userService);
                        break;
                    case 11:
                        handleEnrollSubscription(scanner, subscriptionService);
                        break;
                    case 0:
                        running = false;
                        break;
                    default:
                        System.out.println("Unknown option.");
                }
            } catch (SlotNotAvailableException | DuplicateBookingException | PaymentFailedException e) {
                // The three custom exceptions from §9, caught right here at the CLI boundary
                // for a clean message -- this exact catch block is what a @ControllerAdvice's
                // @ExceptionHandler methods replace later, mapping each type to its own HTTP status.
                System.out.println("Could not complete that action: " + e.getMessage());
            } catch (IllegalArgumentException | IllegalStateException e) {
                // Bad IDs / invalid state transitions -- input-validation-shaped errors, not
                // business rules, so they're plain Java exceptions rather than custom ones.
                System.out.println("Could not complete that action: " + e.getMessage());
            }
        }

        expiryMonitor.stop();
        System.out.println("Goodbye!");
    }

    private static void seedSlots(ParkingSlotService parkingSlotService) {
        parkingSlotService.registerSlot(new ParkingSlot("C-1", 1, SlotType.CAR, true));
        parkingSlotService.registerSlot(new ParkingSlot("C-2", 1, SlotType.CAR, false));
        parkingSlotService.registerSlot(new ParkingSlot("C-3", 1, SlotType.CAR, false));
        parkingSlotService.registerSlot(new ParkingSlot("B-1", 1, SlotType.BIKE, false));
        parkingSlotService.registerSlot(new ParkingSlot("B-2", 1, SlotType.BIKE, false));
        parkingSlotService.registerSlot(new ParkingSlot("T-1", 2, SlotType.TRUCK, false));
    }

    private static void printMenu() {
        System.out.println();
        System.out.println("=== Smart Parking Management System ===");
        System.out.println(" 1. Register a vehicle");
        System.out.println(" 2. Register a customer");
        System.out.println(" 3. View live slot availability");
        System.out.println(" 4. Reserve a slot in advance");
        System.out.println(" 5. Confirm a reservation (vehicle has arrived)");
        System.out.println(" 6. Cancel a reservation");
        System.out.println(" 7. Check in (walk-in, no reservation)");
        System.out.println(" 8. Start EV charging");
        System.out.println(" 9. Stop EV charging");
        System.out.println("10. Check out and pay");
        System.out.println("11. Enroll a vehicle in a subscription");
        System.out.println(" 0. Exit");
    }

    // ---- Menu handlers: each one maps 1:1 to a future REST endpoint. ----

    private static void handleRegisterVehicle(Scanner scanner, VehicleService vehicleService) {
        String plate = readLine(scanner, "License plate: ");
        String ownerName = readLine(scanner, "Owner name: ");
        String ownerContact = readLine(scanner, "Owner contact: ");
        VehicleType type = readVehicleType(scanner);
        boolean electric = readYesNo(scanner, "Is it electric?");
        Vehicle vehicle = vehicleService.register(plate, ownerName, ownerContact, type, electric);
        System.out.println("Registered vehicle with id " + vehicle.getId());
    }

    private static void handleRegisterUser(Scanner scanner, UserService userService) {
        String name = readLine(scanner, "Name: ");
        String email = readLine(scanner, "Email: ");
        User user = userService.register(name, email, UserRole.CUSTOMER);
        System.out.println("Registered user with id " + user.getId());
    }

    private static void handleViewAvailability(ParkingSlotService parkingSlotService) {
        Map<SlotType, Long> availability = parkingSlotService.getLiveAvailability();
        System.out.println("Live availability:");
        availability.forEach((type, count) -> System.out.println("  " + type + ": " + count + " free"));
    }

    private static void handleReserve(Scanner scanner, ReservationService reservationService, VehicleService vehicleService) {
        long vehicleId = readLong(scanner, "Vehicle id: ");
        Vehicle vehicle = vehicleService.getVehicle(vehicleId)
                .orElseThrow(() -> new IllegalArgumentException("No vehicle with id " + vehicleId));
        SlotType slotType = readSlotType(scanner);
        int holdMinutes = readInt(scanner, "Hold for how many minutes: ");
        Reservation reservation = reservationService.reserve(vehicle, slotType, holdMinutes);
        System.out.println("Reservation " + reservation.getId() + " created for slot " + reservation.getSlotId()
                + ", holding until " + reservation.getExpiresAt());
    }

    private static void handleConfirmReservation(Scanner scanner, ReservationService reservationService,
                                                  VehicleService vehicleService, ParkingSlotService parkingSlotService,
                                                  TicketService ticketService) {
        long reservationId = readLong(scanner, "Reservation id: ");
        Reservation reservation = reservationService.confirm(reservationId);
        Vehicle vehicle = vehicleService.getVehicle(reservation.getVehicleId())
                .orElseThrow(() -> new IllegalArgumentException("No vehicle with id " + reservation.getVehicleId()));
        ParkingSlot slot = parkingSlotService.getSlot(reservation.getSlotId())
                .orElseThrow(() -> new IllegalArgumentException("No slot with id " + reservation.getSlotId()));
        // Confirming the reservation and opening the Ticket are two service calls, coordinated
        // here in Main -- exactly the kind of thing a single REST controller method does across
        // multiple services in one request.
        Ticket ticket = ticketService.checkIn(vehicle, slot);
        System.out.println("Reservation confirmed. Checked in -- ticket id " + ticket.getId() + ", slot id " + slot.getId());
    }

    private static void handleCancelReservation(Scanner scanner, ReservationService reservationService) {
        long reservationId = readLong(scanner, "Reservation id: ");
        reservationService.cancel(reservationId);
        System.out.println("Reservation " + reservationId + " cancelled.");
    }

    private static void handleWalkInCheckIn(Scanner scanner, VehicleService vehicleService,
                                             ParkingSlotService parkingSlotService, TicketService ticketService) {
        long vehicleId = readLong(scanner, "Vehicle id: ");
        Vehicle vehicle = vehicleService.getVehicle(vehicleId)
                .orElseThrow(() -> new IllegalArgumentException("No vehicle with id " + vehicleId));
        ParkingSlot slot = parkingSlotService.allocateSlot(vehicle);
        Ticket ticket = ticketService.checkIn(vehicle, slot);
        System.out.println("Checked in -- ticket id " + ticket.getId() + ", slot id " + slot.getId());
    }

    private static void handleStartCharging(Scanner scanner, TicketService ticketService, VehicleService vehicleService,
                                             EVChargingService evChargingService) {
        long ticketId = readLong(scanner, "Ticket id: ");
        Ticket ticket = ticketService.getTicket(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("No ticket with id " + ticketId));
        Vehicle vehicle = vehicleService.getVehicle(ticket.getVehicleId())
                .orElseThrow(() -> new IllegalArgumentException("No vehicle with id " + ticket.getVehicleId()));
        if (!vehicle.isElectric()) {
            System.out.println("This vehicle isn't registered as electric -- nothing to charge.");
            return;
        }
        evChargingService.startCharging(ticket.getId(), ticket.getSlotId());
        System.out.println("Charging started on slot " + ticket.getSlotId());
    }

    private static void handleStopCharging(Scanner scanner, EVChargingService evChargingService) {
        long slotId = readLong(scanner, "Slot id: ");
        System.out.println("Current charge: " + evChargingService.getChargePercent(slotId) + "%");
        PaymentMethod paymentMethod = readPaymentMethod(scanner);
        Payment payment = evChargingService.stopCharging(slotId, paymentMethod);
        System.out.println("Charging stopped. Charging fee: " + payment.getAmount() + " (" + payment.getStatus() + ")");
    }

    private static void handleCheckOut(Scanner scanner, TicketService ticketService,
                                        SubscriptionService subscriptionService, UserService userService) {
        long ticketId = readLong(scanner, "Ticket id: ");
        PricingStrategy pricingStrategy = readPricingStrategy(scanner, subscriptionService);
        PaymentMethod paymentMethod = readPaymentMethod(scanner);
        long userId = readLong(scanner, "User id to notify: ");
        User user = userService.getUser(userId)
                .orElseThrow(() -> new IllegalArgumentException("No user with id " + userId));

        CheckoutReceipt receipt = ticketService.checkOut(ticketId, pricingStrategy, paymentMethod, user);
        System.out.println("Checked out. Fee: " + receipt.getTicket().getTotalFee()
                + ", payment status: " + receipt.getPayment().getStatus());
    }

    private static void handleEnrollSubscription(Scanner scanner, SubscriptionService subscriptionService) {
        long userId = readLong(scanner, "User id: ");
        long vehicleId = readLong(scanner, "Vehicle id: ");
        SubscriptionPlan plan = readYesNo(scanner, "Annual plan? (n for monthly)") ? SubscriptionPlan.ANNUAL : SubscriptionPlan.MONTHLY;
        Subscription subscription = subscriptionService.enroll(userId, vehicleId, plan);
        System.out.println("Subscription " + subscription.getId() + " active until " + subscription.getValidTo());
    }

    // ---- Small composed-input builders for the Strategy objects. ----

    private static PaymentMethod readPaymentMethod(Scanner scanner) {
        System.out.println("Payment method: 1) Card  2) UPI  3) Wallet  4) Cash");
        int choice = readInt(scanner, "Choice: ");
        switch (choice) {
            case 1:
                String cardNumber = readLine(scanner, "Card number (16 digits): ");
                String cvv = readLine(scanner, "CVV (3 digits): ");
                return new CardPayment(cardNumber, cvv);
            case 2:
                String vpa = readLine(scanner, "UPI VPA (e.g. name@bank): ");
                return new UpiPayment(vpa);
            case 3:
                double balance = readDouble(scanner, "Wallet balance available: ");
                return new WalletPayment(balance);
            default:
                return new CashPayment();
        }
    }

    /**
     * Builds a pricing strategy by composing decorators on top of a base rate, in the same
     * order §10's flow describes: base -> optional surge -> optional subscription override.
     */
    private static PricingStrategy readPricingStrategy(Scanner scanner, SubscriptionService subscriptionService) {
        System.out.println("Base rate: 1) Hourly  2) Daily");
        int baseChoice = readInt(scanner, "Choice: ");
        PricingStrategy strategy;
        if (baseChoice == 2) {
            strategy = new DailyPricing(readDouble(scanner, "Rate per day: "));
        } else {
            strategy = new HourlyPricing(readDouble(scanner, "Rate per hour: "));
        }

        if (readYesNo(scanner, "Apply peak-hour surge pricing?")) {
            double multiplier = readDouble(scanner, "Surge multiplier (e.g. 1.5): ");
            int peakStart = readInt(scanner, "Peak start hour (0-23): ");
            int peakEnd = readInt(scanner, "Peak end hour (0-23, exclusive): ");
            strategy = new SurgePricing(strategy, multiplier, peakStart, peakEnd);
        }

        if (readYesNo(scanner, "Waive/discount the fee for active subscribers?")) {
            double flatFee = readDouble(scanner, "Flat fee to charge subscribers (0 to fully waive): ");
            strategy = new SubscriptionPricing(strategy, subscriptionService, flatFee);
        }

        return strategy;
    }

    private static VehicleType readVehicleType(Scanner scanner) {
        while (true) {
            String line = readLine(scanner, "Vehicle type (CAR/BIKE/TRUCK): ").toUpperCase();
            try {
                return VehicleType.valueOf(line);
            } catch (IllegalArgumentException e) {
                System.out.println("Please enter CAR, BIKE, or TRUCK.");
            }
        }
    }

    private static SlotType readSlotType(Scanner scanner) {
        while (true) {
            String line = readLine(scanner, "Slot type (CAR/BIKE/TRUCK): ").toUpperCase();
            try {
                return SlotType.valueOf(line);
            } catch (IllegalArgumentException e) {
                System.out.println("Please enter CAR, BIKE, or TRUCK.");
            }
        }
    }

    // ---- Scanner helpers. All reads go through nextLine() (never mixed with nextInt()) to
    // avoid the classic leftover-newline bug where a stray Enter key gets read as empty input. ----

    private static String readLine(Scanner scanner, String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }

    private static int readInt(Scanner scanner, String prompt) {
        while (true) {
            try {
                return Integer.parseInt(readLine(scanner, prompt));
            } catch (NumberFormatException e) {
                System.out.println("Please enter a whole number.");
            }
        }
    }

    private static long readLong(Scanner scanner, String prompt) {
        while (true) {
            try {
                return Long.parseLong(readLine(scanner, prompt));
            } catch (NumberFormatException e) {
                System.out.println("Please enter a whole number.");
            }
        }
    }

    private static double readDouble(Scanner scanner, String prompt) {
        while (true) {
            try {
                return Double.parseDouble(readLine(scanner, prompt));
            } catch (NumberFormatException e) {
                System.out.println("Please enter a number.");
            }
        }
    }

    private static boolean readYesNo(Scanner scanner, String prompt) {
        while (true) {
            String line = readLine(scanner, prompt + " (y/n): ").toLowerCase();
            if (line.equals("y") || line.equals("yes")) {
                return true;
            }
            if (line.equals("n") || line.equals("no")) {
                return false;
            }
            System.out.println("Please answer y or n.");
        }
    }
}