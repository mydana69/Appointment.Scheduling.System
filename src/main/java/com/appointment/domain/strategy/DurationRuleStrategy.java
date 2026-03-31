package com.appointment.domain.strategy;

import com.appointment.domain.model.Appointment;
import java.time.Duration;
import java.util.Objects;

public class DurationRuleStrategy implements BookingRuleStrategy {

    private final Duration maxDuration;

    public DurationRuleStrategy(Duration maxDuration) {
        this.maxDuration = Objects.requireNonNull(maxDuration, "maxDuration must not be null");
    }

    @Override
    public boolean isValid(Appointment appointment) {
        return appointment.getTimeSlot().getDuration().compareTo(maxDuration) <= 0;
    }

    @Override
    public String getErrorMessage() {
        return "Appointment duration exceeds maximum allowed limit.";
    }
}
