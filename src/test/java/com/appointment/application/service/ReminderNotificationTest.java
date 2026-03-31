package com.appointment.application.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.appointment.application.port.NotificationService;
import com.appointment.application.port.TimeProvider;
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
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ReminderNotificationTest {

    @Test
    void sendUpcomingRemindersNotifiesObservers() {
        LocalDateTime now = LocalDateTime.of(2026, 3, 28, 10, 0);
        TimeSlot nearSlot = new TimeSlot(now.plusMinutes(30), now.plusMinutes(90));
        Schedule schedule = new Schedule(Collections.singletonList(nearSlot));

        InMemoryAppointmentRepository repository = new InMemoryAppointmentRepository();
        TimeProvider timeProvider = Mockito.mock(TimeProvider.class);
        Mockito.when(timeProvider.now()).thenReturn(now);

        List<BookingRuleStrategy> rules = Arrays.asList(
                new DurationRuleStrategy(Duration.ofHours(2)),
                new ParticipantLimitRuleStrategy(8),
                new TypeSpecificRuleStrategy());

        NotificationService notificationService = Mockito.mock(NotificationService.class);
        AppointmentEventPublisher publisher = new AppointmentEventPublisher();
        publisher.register(new NotificationObserver(notificationService, "EMAIL"));

        AppointmentService appointmentService = new AppointmentService(
                repository,
                schedule,
                rules,
                publisher,
                timeProvider,
                new AdminAuthService());

        appointmentService.bookAppointment(new BookingRequest(
                "AP-R1",
                new User("U-9", "Lina"),
                AppointmentType.INDIVIDUAL,
                nearSlot.getStart(),
                nearSlot.getEnd(),
                1));

        appointmentService.sendUpcomingReminders();

        verify(notificationService, times(1)).send(any());
    }
}
