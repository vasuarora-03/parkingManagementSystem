package com.smartparking.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Shared CRUD implementation for every in-memory repository. Package-private on purpose:
 * it's an implementation detail of this layer, not something a service should ever reference
 * (services depend only on the Repository<T,ID> interfaces).
 *
 * Every entity in this system uses a Long id, so ID is fixed here instead of kept generic —
 * that's the one simplification from a fully generic repository, and it matches how every
 * @Entity will declare `@Id @GeneratedValue private Long id;` later anyway.
 *
 * Backed by ConcurrentHashMap rather than a plain HashMap so concurrent reads/writes from
 * multiple threads (e.g. the ReservationExpiryMonitor background thread running alongside
 * CLI-triggered calls) can't corrupt the map itself. Note this only protects the map's own
 * internal structure — it does NOT make multi-step "read then write" sequences atomic. That's
 * why ParkingSlotService still needs its own explicit synchronization for the allocate operation
 * (see §8 / ParkingSlotService) even though the storage underneath is thread-safe.
 *
 * idGetter/idSetter are passed in per subclass (e.g. Vehicle::getId, Vehicle::setId) instead of
 * requiring model classes to implement a shared "Identifiable" interface — that keeps model/
 * classes as plain, framework-free data holders exactly as §4 specifies.
 */
abstract class AbstractInMemoryRepository<T> implements Repository<T, Long> {
    protected final Map<Long, T> store = new ConcurrentHashMap<>();
    private final AtomicLong idSequence = new AtomicLong(0);
    private final Function<T, Long> idGetter;
    private final BiConsumer<T, Long> idSetter;

    protected AbstractInMemoryRepository(Function<T, Long> idGetter, BiConsumer<T, Long> idSetter) {
        this.idGetter = idGetter;
        this.idSetter = idSetter;
    }

    @Override
    public T save(T entity) {
        Long id = idGetter.apply(entity);
        if (id == null) {
            id = idSequence.incrementAndGet();
            idSetter.accept(entity, id);
        }
        store.put(id, entity);
        return entity;
    }

    @Override
    public Optional<T> findById(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<T> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public void deleteById(Long id) {
        store.remove(id);
    }
}