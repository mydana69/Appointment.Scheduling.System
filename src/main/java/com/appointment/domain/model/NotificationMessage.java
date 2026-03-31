package com.appointment.domain.model;

import java.util.Objects;

public class NotificationMessage {

    private final User recipient;

    private final String message;

    public NotificationMessage(User recipient, String message) {
        this.recipient = Objects.requireNonNull(recipient, "recipient must not be null");
        this.message = Objects.requireNonNull(message, "message must not be null");
    }

    public User getRecipient() {
        return recipient;
    }

    public String getMessage() {
        return message;
    }
}
