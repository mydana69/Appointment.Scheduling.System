package com.appointment.domain.model;

import java.util.Objects;

public class Appointment {

    private final String id;

    private final User user;

    private final String accessCode;

    private AppointmentType type;

    private TimeSlot timeSlot;

    private int participants;

    private AppointmentStatus status;

    public Appointment(String id, User user, String accessCode, AppointmentType type, TimeSlot timeSlot, int participants) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.user = Objects.requireNonNull(user, "user must not be null");
        this.accessCode = Objects.requireNonNull(accessCode, "accessCode must not be null");
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.timeSlot = Objects.requireNonNull(timeSlot, "timeSlot must not be null");
        this.participants = participants;
        this.status = AppointmentStatus.CONFIRMED;
    }

    public String getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public String getAccessCode() {
        return accessCode;
    }

    public AppointmentType getType() {
        return type;
    }

    public TimeSlot getTimeSlot() {
        return timeSlot;
    }

    public int getParticipants() {
        return participants;
    }

    public AppointmentStatus getStatus() {
        return status;
    }

    public void cancel() {
        this.status = AppointmentStatus.CANCELLED;
    }

    public void updateDetails(AppointmentType type, TimeSlot timeSlot, int participants) {
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.timeSlot = Objects.requireNonNull(timeSlot, "timeSlot must not be null");
        this.participants = participants;
    }
}
