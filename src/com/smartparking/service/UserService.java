package com.smartparking.service;

import com.smartparking.model.User;
import com.smartparking.model.UserRole;
import com.smartparking.repository.UserRepository;

import java.util.Optional;

/**
 * Not named in §6 — added because Main is barred from touching repositories directly, and
 * something in the service layer has to be able to create/fetch User records now that
 * NotificationChannel.notify(User, ...) needs one. Deliberately minimal: this project has no
 * login/auth flow, so all this needs to do is register and look up.
 */
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User register(String name, String email, UserRole role) {
        return userRepository.save(new User(name, email, role));
    }

    public Optional<User> getUser(Long id) {
        return userRepository.findById(id);
    }
}