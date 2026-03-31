package com.appointment.infrastructure.notification;

import com.appointment.application.port.NotificationService;
import com.appointment.domain.model.NotificationMessage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class InMemoryNotificationService implements NotificationService {

    private final List<NotificationMessage> sentMessages;

    public InMemoryNotificationService() {
        this.sentMessages = new ArrayList<>();
    }

    @Override
    public void send(NotificationMessage message) {
        sentMessages.add(message);
    }

    public List<NotificationMessage> getSentMessages() {
        return Collections.unmodifiableList(sentMessages);
    }
}
