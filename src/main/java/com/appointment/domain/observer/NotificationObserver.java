package com.appointment.domain.observer;

import com.appointment.application.port.NotificationService;
import com.appointment.domain.model.NotificationMessage;
import com.appointment.domain.model.User;
import java.util.Objects;

public class NotificationObserver implements AppointmentObserver {

    private final NotificationService notificationService;

    private final String channel;

    public NotificationObserver(NotificationService notificationService, String channel) {
        this.notificationService = Objects.requireNonNull(notificationService, "notificationService must not be null");
        this.channel = Objects.requireNonNull(channel, "channel must not be null");
    }

    @Override
    public void notify(User user, String message) {
        notificationService.send(new NotificationMessage(user, "[" + channel + "] " + message));
    }
}
