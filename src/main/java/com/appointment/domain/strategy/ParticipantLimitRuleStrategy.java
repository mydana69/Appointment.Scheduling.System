package com.appointment.domain.strategy;

import com.appointment.domain.model.Appointment;

public class ParticipantLimitRuleStrategy implements BookingRuleStrategy {

    private final int maxParticipants;

    public ParticipantLimitRuleStrategy(int maxParticipants) {
        this.maxParticipants = maxParticipants;
    }

    @Override
    public boolean isValid(Appointment appointment) {
        return appointment.getParticipants() > 0 && appointment.getParticipants() <= maxParticipants;
    }

    @Override
    public String getErrorMessage() {
        return "Participant limit exceeded for this appointment.";
    }
}
