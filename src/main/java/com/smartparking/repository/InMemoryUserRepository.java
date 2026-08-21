package com.smartparking.repository;

import com.smartparking.model.User;

public class InMemoryUserRepository extends AbstractInMemoryRepository<User> implements UserRepository {
    public InMemoryUserRepository() {
        super(User::getId, User::setId);
    }
}