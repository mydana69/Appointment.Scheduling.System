package com.appointment.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.appointment.application.port.TimeProvider;
import com.appointment.domain.model.AppointmentType;
import com.appointment.domain.model.BookingRequest;
import com.appointment.domain.model.Schedule;
import com.appointment.domain.model.TimeSlot;
import com.appointment.domain.model.User;
import com.appointment.domain.observer.AppointmentEventPublisher;
import com.appointment.domain.strategy.BookingRuleStrategy;
import com.appointment.domain.strategy.DurationRuleStrategy;
import com.appointment.domain.strategy.ParticipantLimitRuleStrategy;
import com.appointment.domain.strategy.TypeSpecificRuleStrategy;
import com.appointment.persistence.InMemoryAppointmentRepository;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class Sprint1UserStoriesTest {

    private LocalDateTime now;

    private TimeSlot slotA;

    private TimeSlot slotB;

    private AdminAuthService authService;

    private AppointmentService appointmentService;

    @BeforeEach
    void setUp() {
        now = LocalDateTime.of(2026, 3, 28, 10, 0);
        slotA = new TimeSlot(now.plusHours(1), now.plusHours(2));
        slotB = new TimeSlot(now.plusHours(2), now.plusHours(3));
        authService = new AdminAuthService();
        appointmentService = buildService(authService, Arrays.asList(slotA, slotB));
    }

    @Test
    void us11AdministratorLogin() {
        assertTrue(authService.login("admin", "admin123"));
        assertTrue(authService.isLoggedIn());

        authService.logout();

        assertFalse(authService.login("admin", "wrong-password"));
        assertFalse(authService.isLoggedIn());
    }

    @Test
    void us12AdministratorLogoutRequiresReloginForAdminActions() {
        appointmentService.bookAppointment(new BookingRequest(
                "AP-S1-1",
                new User("U-1", "Ali"),
                AppointmentType.INDIVIDUAL,
                slotA.getStart(),
                slotA.getEnd(),
                1));

        authService.login("admin", "admin123");
        authService.logout();

        assertThrows(SecurityException.class, () -> appointmentService.adminCancelReservation("AP-S1-1"));
    }

    @Test
    void us13ViewAvailableSlotsAndBlockFullyBookedSlotSelection() {
        appointmentService.bookAppointment(new BookingRequest(
                "AP-S1-2",
                new User("U-2", "Maya"),
                AppointmentType.INDIVIDUAL,
                slotA.getStart(),
                slotA.getEnd(),
                1));

        List<TimeSlot> availableSlots = appointmentService.viewAvailableSlots();
        assertEquals(1, availableSlots.size());
        assertEquals(slotB.getStart(), availableSlots.get(0).getStart());

        assertThrows(IllegalStateException.class, () -> appointmentService.bookAppointment(new BookingRequest(
                "AP-S1-3",
                new User("U-3", "Samar"),
                AppointmentType.INDIVIDUAL,
                slotA.getStart(),
                slotA.getEnd(),
                1)));
    }

    private AppointmentService buildService(AdminAuthService auth, List<TimeSlot> slots) {
        List<BookingRuleStrategy> rules = Arrays.asList(
                new DurationRuleStrategy(Duration.ofHours(2)),
                new ParticipantLimitRuleStrategy(8),
                new TypeSpecificRuleStrategy());

        TimeProvider timeProvider = () -> now;

        return new AppointmentService(
                new InMemoryAppointmentRepository(),
                new Schedule(slots),
                rules,
                new AppointmentEventPublisher(),
                timeProvider,
                auth);
    }
}
