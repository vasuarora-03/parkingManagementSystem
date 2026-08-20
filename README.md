# Smart Parking Management System

A layered, console-based parking management system written in plain Java (JDK standard
library only — no frameworks, no build tool). It's structured the way a real backend
service would be, so it converts cleanly into a Spring Boot + database app later: the
package structure and dependency direction already match what Spring Boot expects.

## Features

- Register vehicles and customers
- Track a lot's parking slots (car / bike / truck, some EV-equipped) with live availability
- Advance reservations with a time-limited hold that auto-expires if the vehicle never shows
- Walk-in check-in/check-out with duration-based billing
- Composable pricing: hourly or daily rates, optional peak-hour surge, optional subscriber
  discount — all stackable decorators over one `PricingStrategy` interface
- Multiple simulated payment methods (card, UPI, wallet, cash)
- EV charging sessions that run on their own background thread and bill separately
- Console notifications on successful checkout
- Monthly/annual subscriptions that waive or discount the parking fee

## Architecture

```
CLI (Main.java)  -->  Service layer  -->  Repository layer  -->  In-memory store
                          |
                          +--> Strategies: Pricing / Payment / Notification
                          +--> Background threads: ReservationExpiryMonitor, EVChargingSession
```

The CLI only ever calls into the service layer — never a repository directly. Each menu
action maps 1:1 onto a future REST endpoint, and each service method maps 1:1 onto a future
`@RestController` calling a `@Service`.

```
com.smartparking
├── model/          Vehicle, ParkingSlot, Reservation, Ticket, Payment, User, Subscription + enums
├── repository/     Repository<T,ID> + entity-specific interfaces, backed by a shared
│                   in-memory CRUD implementation (ConcurrentHashMap-based)
├── service/        ParkingSlotService, ReservationService, TicketService, PaymentService,
│                   SubscriptionService, EVChargingService, UserService, VehicleService
├── pricing/        PricingStrategy + HourlyPricing, DailyPricing, SurgePricing, SubscriptionPricing
├── payment/        PaymentMethod + CardPayment, UpiPayment, WalletPayment, CashPayment
├── notification/    NotificationChannel + ConsoleNotification
├── concurrency/     ReservationExpiryMonitor (Runnable), EVChargingSession (Thread)
├── exception/       SlotNotAvailableException, DuplicateBookingException, PaymentFailedException
└── Main.java        CLI entry point — Scanner-driven menu, wired only through services
```

### Design patterns

- **Layered architecture** (CLI → Service → Repository → Entity) — the same shape Spring MVC uses.
- **Repository pattern** — every repository is an interface; swapping in Spring Data JPA later
  means deleting the in-memory classes, nothing in the service layer changes.
- **Strategy pattern** — `PricingStrategy`, `PaymentMethod`, and `NotificationChannel` each vary
  behavior without touching calling code.
- **Decorator composition** — `SurgePricing` and `SubscriptionPricing` wrap another
  `PricingStrategy` rather than reimplementing fee math, so they stack:
  `SubscriptionPricing(SurgePricing(HourlyPricing(rate)))`.

### Concurrency

Two background threads:

- `ReservationExpiryMonitor implements Runnable` — one long-lived periodic sweep on a daemon
  thread, started once by `Main`.
- `EVChargingSession extends Thread` — one instance per plugged-in vehicle, each with its own
  identity and lifecycle.

The double-booking race condition (find an available slot, then mark it claimed — a
read-then-write pair that a `ConcurrentHashMap` alone doesn't make atomic) is fixed with a
`synchronized` block around the whole find-then-claim sequence in `ParkingSlotService`, on a
single shared lock object. Verified with 50 threads racing for one free slot: exactly 1
success, 49 correctly rejected. The same check-then-act shape exists one layer up too — "does
this vehicle already have an active reservation/open ticket" followed by saving one — and is
fixed the same way, with a dedicated lock in `ReservationService` and `TicketService`.

## Requirements

- JDK 17+ (built and tested against JDK 21)
- No external dependencies, no build tool required — just `javac`/`java`

## How to run

From the project root:

```bash
# Compile everything into out/
javac -d out $(find src -name "*.java")

# Run the CLI
java -cp out com.smartparking.Main
```

On Windows PowerShell, the compile step is:

```powershell
Get-ChildItem -Recurse -Filter *.java src | ForEach-Object { $_.FullName } | Out-File sources.txt -Encoding utf8
javac -d out "@sources.txt"
java -cp out com.smartparking.Main
```

The lot is seeded at startup with 6 slots (3 car — one with an EV charger, 2 bike, 1 truck).
IDs (vehicle, user, reservation, ticket, slot) are auto-generated and printed after each
action — note them down as you go, since later menu actions ask for them directly.

## Sample flow

1. **Register a vehicle** → note the vehicle id
2. **Register a customer** → note the user id
3. *(optional)* **Enroll the vehicle in a subscription** for a discounted/waived fee later
4. **Reserve a slot in advance**, or skip straight to **Check in (walk-in)**
5. If reserved: **Confirm the reservation** once the vehicle arrives — this both confirms the
   hold and opens the `Ticket`
6. *(optional, EVs only)* **Start EV charging**, wait a bit, then **Stop EV charging** and pay
   the charging fee
7. **Check out and pay** — choose a base rate, optionally layer on surge pricing and/or a
   subscriber discount, choose a payment method, and pick who gets notified

## Roadmap: converting to Spring Boot + a database

| Piece today | Spring Boot version |
|---|---|
| POJOs in `model/` | `@Entity` classes — `@Id @GeneratedValue`, relationships via `@ManyToOne`/`@OneToMany` |
| `Repository<T,ID>` + in-memory impl | `interface XRepository extends JpaRepository<X, Long>` |
| CLI menu in `Main.java` | `@RestController` classes, one per resource |
| Manual `new XService(...)` wiring | Constructor injection, `@Service`/`@Component` |
| `ReservationExpiryMonitor` thread | `@Scheduled(fixedRate = ...)` method |
| Exceptions caught in the CLI | `@ControllerAdvice` + `@ExceptionHandler` → HTTP 404/409/402 |
| No auth | Spring Security + JWT, roles straight from the `UserRole` enum |
| No persistence | PostgreSQL/MySQL via Spring Data JPA, H2 for local dev |
| No API docs | springdoc-openapi (Swagger UI) — nearly free once controllers exist |
