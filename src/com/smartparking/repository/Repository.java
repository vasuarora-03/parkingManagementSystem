package com.smartparking.repository;

import java.util.List;
import java.util.Optional;

/**
 * Deliberately mirrors Spring Data's CrudRepository shape (save / findById / findAll / deleteById).
 * That's not an accident: when this project moves to Spring Boot, every interface below becomes
 * `interface XRepository extends JpaRepository<X, Long>` and the in-memory implementation is
 * simply deleted — no service code has to change because services only ever depend on this
 * interface, never on the concrete in-memory class.
 */
public interface Repository<T, ID> {
    T save(T entity);
    Optional<T> findById(ID id);
    List<T> findAll();
    void deleteById(ID id);
}