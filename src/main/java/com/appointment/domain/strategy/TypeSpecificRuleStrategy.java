package com.appointment.domain.strategy;

import com.appointment.domain.model.Appointment;
import com.appointment.domain.model.AppointmentType;

public class TypeSpecificRuleStrategy implements BookingRuleStrategy {

    @Override
    public boolean isValid(Appointment appointment) {
        AppointmentType type = appointment.getType();
        int participants = appointment.getParticipants();
        switch (type) {
            case INDIVIDUAL:
                return participants == 1;
            case GROUP:
                return participants >= 2;
            case VIRTUAL:
                return participants <= 10;
            default:
                return true;
        }
    }

    @Override
    public String getErrorMessage() {
        return "Appointment type rules were not satisfied.";
    }
}
