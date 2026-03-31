package com.appointment.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.appointment.application.port.TimeProvider;
import com.appointment.application.port.NotificationService;
import com.appointment.domain.model.Appointment;
import com.appointment.domain.model.AppointmentStatus;
import com.appointment.domain.model.AppointmentType;
import com.appointment.domain.model.BookingRequest;
import com.appointment.domain.model.Schedule;
import com.appointment.domain.model.TimeSlot;
import com.appointment.domain.model.User;
import com.appointment.domain.observer.AppointmentEventPublisher;
import com.appointment.domain.observer.NotificationObserver;
import com.appointment.domain.strategy.BookingRuleStrategy;
import com.appointment.domain.strategy.DurationRuleStrategy;
import com.appointment.domain.strategy.ParticipantLimitRuleStrategy;
import com.appointment.domain.strategy.TypeSpecificRuleStrategy;
import com.appointment.persistence.InMemoryAppointmentRepository;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class AppointmentServiceTest {

    private LocalDateTime now;

    private TimeSlot slotA;

    private TimeSlot slotB;

    private AppointmentService appointmentService;

    private InMemoryAppointmentRepository repository;

    @BeforeEach
    void setUp() {
        now = LocalDateTime.of(2026, 3, 28, 10, 0);
        slotA = new TimeSlot(now.plusHours(1), now.plusHours(2));
        slotB = new TimeSlot(now.plusHours(2), now.plusHours(3));

        Schedule schedule = new Schedule(Arrays.asList(slotA, slotB));
        repository = new InMemoryAppointmentRepository();

        List<BookingRuleStrategy> rules = Arrays.asList(
                new DurationRuleStrategy(Duration.ofHours(2)),
                new ParticipantLimitRuleStrategy(8),
                new TypeSpecificRuleStrategy());

        TimeProvider timeProvider = Mockito.mock(TimeProvider.class);
        Mockito.when(timeProvider.now()).thenReturn(now);

        appointmentService = new AppointmentService(
                repository,
                schedule,
                rules,
                new AppointmentEventPublisher(),
                timeProvider,
                new AdminAuthService());
    }

    @Test
    void bookAppointmentSavesConfirmedAppointment() {
        BookingRequest request = new BookingRequest(
                "AP-1",
                new User("U-1", "Maya"),
                AppointmentType.INDIVIDUAL,
                slotA.getStart(),
                slotA.getEnd(),
                1);

        Appointment appointment = appointmentService.bookAppointment(request);

        assertEquals(AppointmentStatus.CONFIRMED, appointment.getStatus());
        assertTrue(repository.findById("AP-1").isPresent());
    }

    @Test
    void durationRuleRejectsLongAppointment() {
        BookingRequest request = new BookingRequest(
                "AP-2",
                new User("U-2", "Hadi"),
                AppointmentType.INDIVIDUAL,
                slotA.getStart(),
                slotA.getEnd().plusHours(2),
                1);

        assertThrows(IllegalArgumentException.class, () -> appointmentService.bookAppointment(request));
    }

    @Test
    void participantRuleRejectsExcessParticipants() {
        BookingRequest request = new BookingRequest(
                "AP-3",
                new User("U-3", "Samar"),
                AppointmentType.GROUP,
                slotA.getStart(),
                slotA.getEnd(),
                15);

        assertThrows(IllegalArgumentException.class, () -> appointmentService.bookAppointment(request));
    }

    @Test
    void bookingRejectsUserIdReusedByDifferentName() {
        appointmentService.bookAppointment(new BookingRequest(
                "AP-USER-1",
                new User("U-77", "Nour"),
                AppointmentType.INDIVIDUAL,
                slotA.getStart(),
                slotA.getEnd(),
                1));

        BookingRequest conflicting = new BookingRequest(
                "AP-USER-2",
                new User("U-77", "Sami"),
                AppointmentType.INDIVIDUAL,
                slotB.getStart(),
                slotB.getEnd(),
                1);

        assertThrows(IllegalArgumentException.class, () -> appointmentService.bookAppointment(conflicting));
    }

    @Test
    void bookingRejectsAlreadyRegisteredActiveUser() {
        appointmentService.bookAppointment(new BookingRequest(
                "AP-USER-3",
                new User("U-99", "Samar"),
                AppointmentType.INDIVIDUAL,
                slotA.getStart(),
                slotA.getEnd(),
                1));

        BookingRequest duplicate = new BookingRequest(
                "AP-USER-4",
                new User("U-99", "Samar"),
                AppointmentType.INDIVIDUAL,
                slotB.getStart(),
                slotB.getEnd(),
                1);

        assertThrows(IllegalStateException.class, () -> appointmentService.bookAppointment(duplicate));
    }

    @Test
    void viewAvailableSlotsHidesBookedSlots() {
        appointmentService.bookAppointment(new BookingRequest(
                "AP-4",
                new User("U-4", "Ali"),
                AppointmentType.INDIVIDUAL,
                slotA.getStart(),
                slotA.getEnd(),
                1));

        List<TimeSlot> availableSlots = appointmentService.viewAvailableSlots();

        assertEquals(1, availableSlots.size());
        assertEquals(slotB.getStart(), availableSlots.get(0).getStart());
    }

    @Test
    void cancellationReleasesSlot() {
        Appointment appointment = appointmentService.bookAppointment(new BookingRequest(
                "AP-5",
                new User("U-5", "Rana"),
                AppointmentType.INDIVIDUAL,
                slotA.getStart(),
                slotA.getEnd(),
                1));

        appointmentService.cancelAppointment("AP-5", appointment.getAccessCode());

        assertFalse(appointmentService.viewAvailableSlots().isEmpty());
    }

    @Test
    void cancellationRejectsInvalidAccessCode() {
        appointmentService.bookAppointment(new BookingRequest(
                "AP-5X",
                new User("U-5", "Rana"),
                AppointmentType.INDIVIDUAL,
                slotA.getStart(),
                slotA.getEnd(),
                1));

        assertThrows(SecurityException.class, () -> appointmentService.cancelAppointment("AP-5X", "WRONG1"));
    }

    @Test
    void adminActionsRequireActiveLoginAfterLogout() {
        AdminAuthService authService = new AdminAuthService();
        authService.login("admin", "admin123");
        authService.logout();

        TimeProvider timeProvider = Mockito.mock(TimeProvider.class);
        Mockito.when(timeProvider.now()).thenReturn(now);
        AppointmentService securedService = new AppointmentService(
                repository,
                new Schedule(Arrays.asList(slotA, slotB)),
                Arrays.asList(
                        new DurationRuleStrategy(Duration.ofHours(2)),
                        new ParticipantLimitRuleStrategy(8),
                        new TypeSpecificRuleStrategy()),
                new AppointmentEventPublisher(),
                timeProvider,
                authService);

        assertThrows(SecurityException.class, () -> securedService.adminCancelReservation("AP-5"));
    }

    @Test
    void modifyRejectsPastAppointments() {
        TimeSlot pastSlot = new TimeSlot(now.minusHours(2), now.minusHours(1));
        Schedule pastSchedule = new Schedule(Collections.singletonList(pastSlot));

        TimeProvider timeProvider = Mockito.mock(TimeProvider.class);
        Mockito.when(timeProvider.now()).thenReturn(now);

        AppointmentService pastService = new AppointmentService(
                new InMemoryAppointmentRepository(),
                pastSchedule,
                Arrays.asList(
                        new DurationRuleStrategy(Duration.ofHours(2)),
                        new ParticipantLimitRuleStrategy(8),
                        new TypeSpecificRuleStrategy()),
                new AppointmentEventPublisher(),
                timeProvider,
                new AdminAuthService());

        Appointment appointment = pastService.bookAppointment(new BookingRequest(
                "AP-6",
                new User("U-6", "Nour"),
                AppointmentType.INDIVIDUAL,
                pastSlot.getStart(),
                pastSlot.getEnd(),
                1));

        assertThrows(IllegalStateException.class, () -> pastService.modifyAppointment("AP-6", appointment.getAccessCode(), new BookingRequest(
                "AP-6",
                new User("U-6", "Nour"),
                AppointmentType.INDIVIDUAL,
                pastSlot.getStart(),
                pastSlot.getEnd(),
                1)));
    }

    @Test
    void modifyRejectsInvalidAccessCode() {
        appointmentService.bookAppointment(new BookingRequest(
                "AP-7",
                new User("U-7", "Jana"),
                AppointmentType.INDIVIDUAL,
                slotA.getStart(),
                slotA.getEnd(),
                1));

        BookingRequest update = new BookingRequest(
                "AP-7",
                new User("U-7", "Jana"),
                AppointmentType.INDIVIDUAL,
                slotB.getStart(),
                slotB.getEnd(),
                1);

        assertThrows(SecurityException.class, () -> appointmentService.modifyAppointment("AP-7", "WRONG1", update));
    }

    @Test
    void adminCancelSendsNotification() {
        NotificationService notificationService = Mockito.mock(NotificationService.class);
        AppointmentEventPublisher publisher = new AppointmentEventPublisher();
        publisher.register(new NotificationObserver(notificationService, "EMAIL"));

        AdminAuthService auth = new AdminAuthService();
        auth.login("admin", "admin123");

        AppointmentService service = new AppointmentService(
                new InMemoryAppointmentRepository(),
                new Schedule(Arrays.asList(slotA, slotB)),
                Arrays.asList(
                        new DurationRuleStrategy(Duration.ofHours(2)),
                        new ParticipantLimitRuleStrategy(8),
                        new TypeSpecificRuleStrategy()),
                publisher,
                () -> now,
                auth);

        service.bookAppointment(new BookingRequest(
                "AP-8",
                new User("U-8", "Rami"),
                AppointmentType.INDIVIDUAL,
                slotA.getStart(),
                slotA.getEnd(),
                1));

        service.adminCancelReservation("AP-8");

        verify(notificationService, times(1)).send(any());
    }

    @Test
    void adminModifySendsNotification() {
        NotificationService notificationService = Mockito.mock(NotificationService.class);
        AppointmentEventPublisher publisher = new AppointmentEventPublisher();
        publisher.register(new NotificationObserver(notificationService, "EMAIL"));

        AdminAuthService auth = new AdminAuthService();
        auth.login("admin", "admin123");

        AppointmentService service = new AppointmentService(
                new InMemoryAppointmentRepository(),
                new Schedule(Arrays.asList(slotA, slotB)),
                Arrays.asList(
                        new DurationRuleStrategy(Duration.ofHours(2)),
                        new ParticipantLimitRuleStrategy(8),
                        new TypeSpecificRuleStrategy()),
                publisher,
                () -> now,
                auth);

        service.bookAppointment(new BookingRequest(
                "AP-9",
                new User("U-9", "Rama"),
                AppointmentType.INDIVIDUAL,
                slotA.getStart(),
                slotA.getEnd(),
                1));

        service.adminModifyReservation("AP-9", new BookingRequest(
                "AP-9",
                new User("U-9", "Rama"),
                AppointmentType.GROUP,
                slotB.getStart(),
                slotB.getEnd(),
                2));

        verify(notificationService, atLeastOnce()).send(any());
    }

    @Test
    void remindersUseTwentyFourHourWindow() {
        TimeSlot nearSlot = new TimeSlot(now.plusHours(23), now.plusHours(24));
        TimeSlot farSlot = new TimeSlot(now.plusHours(26), now.plusHours(27));

        NotificationService notificationService = Mockito.mock(NotificationService.class);
        AppointmentEventPublisher publisher = new AppointmentEventPublisher();
        publisher.register(new NotificationObserver(notificationService, "EMAIL"));

        AppointmentService service = new AppointmentService(
                new InMemoryAppointmentRepository(),
                new Schedule(Arrays.asList(nearSlot, farSlot)),
                Arrays.asList(
                        new DurationRuleStrategy(Duration.ofHours(2)),
                        new ParticipantLimitRuleStrategy(8),
                        new TypeSpecificRuleStrategy()),
                publisher,
                () -> now,
                new AdminAuthService());

        service.bookAppointment(new BookingRequest(
                "AP-10",
                new User("U-10", "Salma"),
                AppointmentType.INDIVIDUAL,
                nearSlot.getStart(),
                nearSlot.getEnd(),
                1));

        service.bookAppointment(new BookingRequest(
                "AP-11",
                new User("U-11", "Kareem"),
                AppointmentType.INDIVIDUAL,
                farSlot.getStart(),
                farSlot.getEnd(),
                1));

        service.sendUpcomingReminders();

        verify(notificationService, times(1)).send(any());
    }
}
