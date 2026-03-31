package com.appointment.domain.observer;

import com.appointment.domain.model.User;
import java.util.ArrayList;
import java.util.List;

public class AppointmentEventPublisher {

    private final List<AppointmentObserver> observers;

    public AppointmentEventPublisher() {
        this.observers = new ArrayList<>();
    }

    public void register(AppointmentObserver observer) {
        observers.add(observer);
    }

    public void notifyAllObservers(User user, String message) {
        for (AppointmentObserver observer : observers) {
            observer.notify(user, message);
        }
    }
}
