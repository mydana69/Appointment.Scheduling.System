package com.appointment;

import com.appointment.application.service.AdminAuthService;
import com.appointment.application.service.AppointmentService;
import com.appointment.domain.model.Appointment;
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
import com.appointment.infrastructure.notification.FileNotificationService;
import com.appointment.infrastructure.time.SystemTimeProvider;
import com.appointment.persistence.FileAppointmentRepository;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

public class App {

    public static void main(String[] args) {
        LocalDateTime now = LocalDateTime.now().withSecond(0).withNano(0);
        List<TimeSlot> slots = Arrays.asList(
                new TimeSlot(now.plusHours(2), now.plusHours(3)),
                new TimeSlot(now.plusHours(4), now.plusHours(5)));

        Schedule schedule = new Schedule(slots);
        FileAppointmentRepository repository = new FileAppointmentRepository();
        FileNotificationService notificationService = new FileNotificationService();

        AppointmentEventPublisher publisher = new AppointmentEventPublisher();
        publisher.register(new NotificationObserver(notificationService, "EMAIL"));
        publisher.register(new NotificationObserver(notificationService, "SMS"));

        List<BookingRuleStrategy> rules = Arrays.asList(
                new DurationRuleStrategy(Duration.ofHours(2)),
                new ParticipantLimitRuleStrategy(8),
                new TypeSpecificRuleStrategy());

        AdminAuthService authService = new AdminAuthService();
        AppointmentService appointmentService = new AppointmentService(
                repository,
                schedule,
                rules,
                publisher,
                new SystemTimeProvider(),
                authService);

        User user = new User("U-1", "Alice");
        BookingRequest request = new BookingRequest(
                "AP-100",
                user,
                AppointmentType.INDIVIDUAL,
                slots.get(0).getStart(),
                slots.get(0).getEnd(),
                1);

        Appointment appointment = appointmentService.bookAppointment(request);
        System.out.println("Booked appointment status: " + appointment.getStatus());
        System.out.println("Available slots after booking: " + appointmentService.viewAvailableSlots().size());
    }
}
