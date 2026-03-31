package com.appointment.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.appointment.domain.model.Appointment;
import com.appointment.domain.model.AppointmentStatus;
import com.appointment.domain.model.AppointmentType;
import com.appointment.domain.model.TimeSlot;
import com.appointment.domain.model.User;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.Test;

class FileAppointmentRepositoryTest {

    @Test
    void saveThenReloadRestoresAppointments() throws Exception {
        Path tempDir = Files.createTempDirectory("appt-repo-test-");
        Path dbFile = tempDir.resolve("system.db");

        FileAppointmentRepository repository = new FileAppointmentRepository(dbFile);
        LocalDateTime now = LocalDateTime.of(2026, 3, 28, 10, 0);

        Appointment confirmed = new Appointment(
                "AP-FILE-1",
                new User("U-1", "Ali"),
                "ZXCV12",
                AppointmentType.INDIVIDUAL,
                new TimeSlot(now.plusHours(1), now.plusHours(2)),
                1);
        Appointment cancelled = new Appointment(
                "AP-FILE-2",
                new User("U-2", "Maya"),
                "QWER34",
                AppointmentType.GROUP,
                new TimeSlot(now.plusHours(3), now.plusHours(4)),
                2);
        cancelled.cancel();

        repository.save(confirmed);
        repository.save(cancelled);

        FileAppointmentRepository reloaded = new FileAppointmentRepository(dbFile);
        List<Appointment> appointments = reloaded.findAll();

        assertEquals(2, appointments.size());
        Appointment loaded = reloaded.findById("AP-FILE-2").orElseThrow(AssertionError::new);
        assertEquals("QWER34", loaded.getAccessCode());
        assertEquals(AppointmentStatus.CANCELLED, loaded.getStatus());
    }

    @Test
    void savePreservesNotificationLines() throws Exception {
        Path tempDir = Files.createTempDirectory("appt-repo-preserve-");
        Path dbFile = tempDir.resolve("system.db");

        String notifHeader = "NOTIF_HEADER|recipientIdB64|recipientNameB64|messageB64";
        String notifLine = "NOTIF|" + encode("U-7") + "|" + encode("Sara") + "|" + encode("hello");
        Files.write(dbFile, (notifHeader + System.lineSeparator() + notifLine + System.lineSeparator()).getBytes(StandardCharsets.UTF_8));

        FileAppointmentRepository repository = new FileAppointmentRepository(dbFile);
        LocalDateTime now = LocalDateTime.of(2026, 3, 28, 10, 0);
        repository.save(new Appointment(
                "AP-FILE-3",
                new User("U-3", "Nour"),
                "AAAA11",
                AppointmentType.INDIVIDUAL,
                new TimeSlot(now.plusHours(1), now.plusHours(2)),
                1));

        List<String> lines = Files.readAllLines(dbFile, StandardCharsets.UTF_8);
        assertTrue(lines.stream().anyMatch(notifLine::equals));
    }

    @Test
    void loadLegacyRecordWithoutAccessCode() throws Exception {
        Path tempDir = Files.createTempDirectory("appt-repo-legacy-");
        Path dbFile = tempDir.resolve("system.db");

        String legacyLine = "APPT|"
                + encode("AP-LEGACY") + "|"
                + encode("U-L") + "|"
                + encode("Legacy User") + "|"
                + "INDIVIDUAL|"
                + "2026-03-29T11:00|"
                + "2026-03-29T12:00|"
                + "1|"
                + "CONFIRMED";

        Files.write(dbFile, (legacyLine + System.lineSeparator()).getBytes(StandardCharsets.UTF_8));

        FileAppointmentRepository repository = new FileAppointmentRepository(dbFile);
        Appointment loaded = repository.findById("AP-LEGACY").orElseThrow(AssertionError::new);

        assertTrue(loaded.getAccessCode().startsWith("LEGACY-"));
        assertEquals("U-L", loaded.getUser().getId());
    }

    @Test
    void malformedAppointmentRecordThrows() throws Exception {
        Path tempDir = Files.createTempDirectory("appt-repo-bad-");
        Path dbFile = tempDir.resolve("system.db");
        Files.write(dbFile, ("APPT|BROKEN" + System.lineSeparator()).getBytes(StandardCharsets.UTF_8));

        assertThrows(IllegalStateException.class, () -> new FileAppointmentRepository(dbFile));
    }

    private static String encode(String value) {
        return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }
}
