package com.appointment.domain.strategy;

import com.appointment.domain.model.Appointment;

public interface BookingRuleStrategy {

    boolean isValid(Appointment appointment);

    String getErrorMessage();
}
