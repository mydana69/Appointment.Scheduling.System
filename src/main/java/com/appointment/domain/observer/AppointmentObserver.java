package com.appointment.domain.observer;

import com.appointment.domain.model.User;

public interface AppointmentObserver {

    void notify(User user, String message);
}
