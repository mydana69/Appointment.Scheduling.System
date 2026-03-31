package com.appointment.application.port;

import com.appointment.domain.model.NotificationMessage;

public interface NotificationService {

    void send(NotificationMessage message);
}
