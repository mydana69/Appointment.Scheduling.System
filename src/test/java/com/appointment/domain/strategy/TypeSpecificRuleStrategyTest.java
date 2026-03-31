package com.appointment.domain.strategy;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.appointment.domain.model.Appointment;
import com.appointment.domain.model.AppointmentType;
import com.appointment.domain.model.TimeSlot;
import com.appointment.domain.model.User;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class TypeSpecificRuleStrategyTest {

    @Test
    void individualRequiresExactlyOneParticipant() {
        TypeSpecificRuleStrategy strategy = new TypeSpecificRuleStrategy();

        assertTrue(strategy.isValid(appointment(AppointmentType.INDIVIDUAL, 1)));
        assertFalse(strategy.isValid(appointment(AppointmentType.INDIVIDUAL, 2)));
    }

    @Test
    void groupRequiresAtLeastTwoParticipants() {
        TypeSpecificRuleStrategy strategy = new TypeSpecificRuleStrategy();

        assertFalse(strategy.isValid(appointment(AppointmentType.GROUP, 1)));
        assertTrue(strategy.isValid(appointment(AppointmentType.GROUP, 2)));
    }

    @Test
    void virtualAllowsUpToTenParticipants() {
        TypeSpecificRuleStrategy strategy = new TypeSpecificRuleStrategy();

        assertTrue(strategy.isValid(appointment(AppointmentType.VIRTUAL, 10)));
        assertFalse(strategy.isValid(appointment(AppointmentType.VIRTUAL, 11)));
    }

    @Test
    void unrestrictedTypesPassValidation() {
        TypeSpecificRuleStrategy strategy = new TypeSpecificRuleStrategy();

        assertTrue(strategy.isValid(appointment(AppointmentType.URGENT, 20)));
    }

    private static Appointment appointment(AppointmentType type, int participants) {
        LocalDateTime now = LocalDateTime.of(2026, 3, 28, 10, 0);
        return new Appointment("AP-TYPE-1", new User("U-1", "Ali"), "ABC123", type,
                new TimeSlot(now.plusHours(1), now.plusHours(2)), participants);
    }
}
