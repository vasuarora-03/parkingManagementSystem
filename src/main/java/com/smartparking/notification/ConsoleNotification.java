package com.smartparking.notification;

import com.smartparking.model.User;

/** Today's stand-in for a real delivery channel — prints to the CLI instead of sending anything externally. */
public class ConsoleNotification implements NotificationChannel {
    @Override
    public void notify(User user, String message) {
        System.out.println("[notification -> " + user.getName() + " <" + user.getEmail() + ">] " + message);
    }
}