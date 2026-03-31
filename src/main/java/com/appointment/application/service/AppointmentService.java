package com.appointment.application.service;

import com.appointment.application.port.TimeProvider;
import com.appointment.domain.model.Appointment;
import com.appointment.domain.model.AppointmentStatus;
import com.appointment.domain.model.AppointmentType;
import com.appointment.domain.model.BookingRequest;
import com.appointment.domain.model.Schedule;
import com.appointment.domain.model.TimeSlot;
import com.appointment.domain.observer.AppointmentEventPublisher;
import com.appointment.domain.strategy.BookingRuleStrategy;
import com.appointment.persistence.AppointmentRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

public class AppointmentService {

    private final AppointmentRepository appointmentRepository;

    private final Schedule schedule;

    private final List<BookingRuleStrategy> bookingRules;

    private final AppointmentEventPublisher eventPublisher;

    private final TimeProvider timeProvider;

    private final AdminAuthService adminAuthService;

    public AppointmentService(AppointmentRepository appointmentRepository, Schedule schedule,
            List<BookingRuleStrategy> bookingRules, AppointmentEventPublisher eventPublisher, TimeProvider timeProvider,
            AdminAuthService adminAuthService) {
        this.appointmentRepository = Objects.requireNonNull(appointmentRepository, "appointmentRepository must not be null");
        this.schedule = Objects.requireNonNull(schedule, "schedule must not be null");
        this.bookingRules = new ArrayList<>(Objects.requireNonNull(bookingRules, "bookingRules must not be null"));
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher must not be null");
        this.timeProvider = Objects.requireNonNull(timeProvider, "timeProvider must not be null");
        this.adminAuthService = Objects.requireNonNull(adminAuthService, "adminAuthService must not be null");
    }

    public Appointment bookAppointment(BookingRequest request) {
        TimeSlot slot = new TimeSlot(request.getStart(), request.getEnd());
        assertSlotConfigured(slot);
        assertSlotFree(slot, Optional.empty());
        assertUserIdentityConsistency(request);

        Appointment appointment = new Appointment(
                request.getAppointmentId(),
                request.getUser(),
                generateAccessCode(),
                request.getType(),
                slot,
                request.getParticipants());
        validateRules(appointment);

        appointmentRepository.save(appointment);
        return appointment;
    }

    private void assertUserIdentityConsistency(BookingRequest request) {
        String requestUserId = request.getUser().getId();
        String requestUserName = request.getUser().getName().trim();

        boolean alreadyRegistered = appointmentRepository.findAll().stream()
                .filter(a -> a.getStatus() == AppointmentStatus.CONFIRMED)
                .map(Appointment::getUser)
                .anyMatch(existingUser -> existingUser.getId().equals(requestUserId)
                && existingUser.getName().trim().equalsIgnoreCase(requestUserName));

        if (alreadyRegistered) {
            throw new IllegalStateException("User already registered with an active appointment.");
        }

        boolean conflictExists = appointmentRepository.findAll().stream()
                .map(Appointment::getUser)
                .anyMatch(existingUser -> existingUser.getId().equals(requestUserId)
                && !existingUser.getName().trim().equalsIgnoreCase(requestUserName));

        if (conflictExists) {
            throw new IllegalArgumentException("User ID already exists for another user name.");
        }
    }

    public List<TimeSlot> viewAvailableSlots() {
        List<TimeSlot> available = new ArrayList<>();
        for (TimeSlot configuredSlot : schedule.getSlots()) {
            boolean occupied = appointmentRepository.findAll().stream()
                    .filter(a -> a.getStatus() == AppointmentStatus.CONFIRMED)
                    .anyMatch(a -> a.getTimeSlot().overlaps(configuredSlot));
            if (!occupied) {
                available.add(configuredSlot);
            }
        }
        return available;
    }

    public List<Appointment> viewAppointments() {
        return new ArrayList<>(appointmentRepository.findAll());
    }

    public void cancelAppointment(String appointmentId, String accessCode) {
        Appointment appointment = findAppointmentOrThrow(appointmentId);
        assertFutureAppointment(appointment);
        assertAccessCode(appointment, accessCode);
        appointment.cancel();
        appointmentRepository.save(appointment);
    }

    public Appointment modifyAppointment(String appointmentId, String accessCode, BookingRequest updatedRequest) {
        Appointment appointment = findAppointmentOrThrow(appointmentId);
        assertFutureAppointment(appointment);
        assertAccessCode(appointment, accessCode);

        TimeSlot updatedSlot = new TimeSlot(updatedRequest.getStart(), updatedRequest.getEnd());
        assertSlotConfigured(updatedSlot);
        assertSlotFree(updatedSlot, Optional.of(appointmentId));

        appointment.updateDetails(updatedRequest.getType(), updatedSlot, updatedRequest.getParticipants());
        validateRules(appointment);
        appointmentRepository.save(appointment);
        return appointment;
    }

    public void adminCancelReservation(String appointmentId) {
        requireAdminSession();
        Appointment appointment = findAppointmentOrThrow(appointmentId);
        assertFutureAppointment(appointment);
        appointment.cancel();
        appointmentRepository.save(appointment);
        notifyAdminChange(
                appointment,
                "Admin cancelled your appointment " + appointment.getId()
                + ". The slot is now released. [CODE:" + appointment.getAccessCode() + "]");
    }

    public Appointment adminModifyReservation(String appointmentId, BookingRequest updatedRequest) {
        requireAdminSession();
        Appointment appointment = findAppointmentOrThrow(appointmentId);
        assertFutureAppointment(appointment);

        TimeSlot previousSlot = appointment.getTimeSlot();
        AppointmentType previousType = appointment.getType();
        int previousParticipants = appointment.getParticipants();

        TimeSlot updatedSlot = new TimeSlot(updatedRequest.getStart(), updatedRequest.getEnd());
        assertSlotConfigured(updatedSlot);
        assertSlotFree(updatedSlot, Optional.of(appointmentId));

        appointment.updateDetails(updatedRequest.getType(), updatedSlot, updatedRequest.getParticipants());
        validateRules(appointment);
        appointmentRepository.save(appointment);

        String message = "Admin modified appointment " + appointment.getId()
                + ". Old: [" + previousType + ", " + previousSlot.getStart() + " -> " + previousSlot.getEnd()
                + ", participants=" + previousParticipants + "]"
                + ". New: [" + appointment.getType() + ", " + appointment.getTimeSlot().getStart() + " -> "
                + appointment.getTimeSlot().getEnd() + ", participants=" + appointment.getParticipants() + "]"
                + ". [CODE:" + appointment.getAccessCode() + "]";
        notifyAdminChange(appointment, message);
        return appointment;
    }

    public void sendUpcomingReminders() {
        LocalDateTime now = timeProvider.now();
        LocalDateTime horizon = now.plusHours(24);
        for (Appointment appointment : appointmentRepository.findAll()) {
            if (appointment.getStatus() != AppointmentStatus.CONFIRMED) {
                continue;
            }
            LocalDateTime start = appointment.getTimeSlot().getStart();
            if ((start.isEqual(now) || start.isAfter(now)) && (start.isBefore(horizon) || start.isEqual(horizon))) {
                String message = "Reminder: appointment " + appointment.getId() + " starts at " + start
                        + ". [CODE:" + appointment.getAccessCode() + "]";
                eventPublisher.notifyAllObservers(appointment.getUser(), message);
            }
        }
    }

    private void assertAccessCode(Appointment appointment, String accessCode) {
        String provided = Objects.requireNonNull(accessCode, "accessCode must not be null").trim().toUpperCase(Locale.ROOT);
        if (provided.isEmpty() || !appointment.getAccessCode().equals(provided)) {
            throw new SecurityException("Invalid appointment access code.");
        }
    }

    private void notifyAdminChange(Appointment appointment, String message) {
        eventPublisher.notifyAllObservers(appointment.getUser(), message);
    }

    private String generateAccessCode() {
        final String alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder builder = new StringBuilder(6);
        for (int i = 0; i < 6; i++) {
            int index = ThreadLocalRandom.current().nextInt(alphabet.length());
            builder.append(alphabet.charAt(index));
        }
        return builder.toString();
    }

    private void validateRules(Appointment appointment) {
        for (BookingRuleStrategy rule : bookingRules) {
            if (!rule.isValid(appointment)) {
                throw new IllegalArgumentException(rule.getErrorMessage());
            }
        }
    }

    private void assertSlotConfigured(TimeSlot selectedSlot) {
        boolean configured = schedule.getSlots().stream()
                .anyMatch(slot -> slot.getStart().equals(selectedSlot.getStart()) && slot.getEnd().equals(selectedSlot.getEnd()));
        if (!configured) {
            throw new IllegalArgumentException("Selected slot does not exist in schedule.");
        }
    }

    private void assertSlotFree(TimeSlot selectedSlot, Optional<String> excludedAppointmentId) {
        boolean occupied = appointmentRepository.findAll().stream()
                .filter(a -> a.getStatus() == AppointmentStatus.CONFIRMED)
                .filter(a -> excludedAppointmentId.map(id -> !id.equals(a.getId())).orElse(true))
                .anyMatch(a -> a.getTimeSlot().overlaps(selectedSlot));
        if (occupied) {
            throw new IllegalStateException("Selected slot is fully booked.");
        }
    }

    private Appointment findAppointmentOrThrow(String appointmentId) {
        return appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found: " + appointmentId));
    }

    private void assertFutureAppointment(Appointment appointment) {
        if (!appointment.getTimeSlot().getStart().isAfter(timeProvider.now())) {
            throw new IllegalStateException("Only future appointments can be modified or cancelled.");
        }
    }

    private void requireAdminSession() {
        if (!adminAuthService.isLoggedIn()) {
            throw new SecurityException("Administrator must log in first.");
        }
    }
}
