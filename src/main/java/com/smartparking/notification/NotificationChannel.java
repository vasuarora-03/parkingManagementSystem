package com.smartparking.notification;

import com.smartparking.model.User;

/**
 * Why this interface exists: WHO gets notified stays the same (a User + a message), but HOW
 * genuinely differs — console output today, an SMTP call or an SMS gateway call tomorrow.
 * TicketService depends only on this interface, so swapping ConsoleNotification for
 * EmailNotification later is a one-line wiring change in Main, not a change to any service.
 */
public interface NotificationChannel {
    void notify(User user, String message);
}