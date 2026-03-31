package com.appointment.domain.model;

import java.time.LocalDateTime;
import java.util.Objects;

public class BookingRequest {

    private final String appointmentId;

    private final User user;

    private final AppointmentType type;

    private final LocalDateTime start;

    private final LocalDateTime end;

    private final int participants;

    public BookingRequest(String appointmentId, User user, AppointmentType type, LocalDateTime start, LocalDateTime end,
            int participants) {
        this.appointmentId = Objects.requireNonNull(appointmentId, "appointmentId must not be null");
        this.user = Objects.requireNonNull(user, "user must not be null");
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.start = Objects.requireNonNull(start, "start must not be null");
        this.end = Objects.requireNonNull(end, "end must not be null");
        this.participants = participants;
    }

    public String getAppointmentId() {
        return appointmentId;
    }

    public User getUser() {
        return user;
    }

    public AppointmentType getType() {
        return type;
    }

    public LocalDateTime getStart() {
        return start;
    }

    public LocalDateTime getEnd() {
        return end;
    }

    public int getParticipants() {
        return participants;
    }
}
