package com.appointment.persistence;

import com.appointment.domain.model.Appointment;
import com.appointment.domain.model.AppointmentStatus;
import com.appointment.domain.model.AppointmentType;
import com.appointment.domain.model.TimeSlot;
import com.appointment.domain.model.User;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class FileAppointmentRepository implements AppointmentRepository {

    private static final String HEADER = "APPT_HEADER|id|accessCodeB64|userIdB64|userNameB64|type|start|end|participants|status";

    private static final String RECORD_PREFIX = "APPT|";

    private final Path filePath;

    private final Map<String, Appointment> storage;

    public FileAppointmentRepository() {
        this(Paths.get(System.getProperty("appointment.storage.file", "data/system.db")));
    }

    public FileAppointmentRepository(Path filePath) {
        this.filePath = Objects.requireNonNull(filePath, "filePath must not be null");
        this.storage = new LinkedHashMap<>();
        initializeStorageFile();
        loadFromDisk();
    }

    @Override
    public synchronized void save(Appointment appointment) {
        storage.put(appointment.getId(), appointment);
        flushToDisk();
    }

    @Override
    public synchronized Optional<Appointment> findById(String id) {
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public synchronized List<Appointment> findAll() {
        return new ArrayList<>(storage.values());
    }

    private void initializeStorageFile() {
        try {
            Path parent = filePath.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            if (!Files.exists(filePath)) {
                Files.write(filePath, new byte[0], StandardOpenOption.CREATE_NEW);
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to initialize appointment storage file.", ex);
        }
    }

    private void loadFromDisk() {
        try {
            List<String> lines = Files.readAllLines(filePath, StandardCharsets.UTF_8);
            for (String line : lines) {
                if (!line.startsWith(RECORD_PREFIX)) {
                    continue;
                }
                Appointment appointment = deserialize(line);
                storage.put(appointment.getId(), appointment);
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to load appointments from file.", ex);
        }
    }

    private void flushToDisk() {
        List<String> preserved = new ArrayList<>();
        try {
            for (String line : Files.readAllLines(filePath, StandardCharsets.UTF_8)) {
                if (!line.startsWith(RECORD_PREFIX) && !HEADER.equals(line)) {
                    preserved.add(line);
                }
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read existing storage before saving appointments.", ex);
        }

        List<String> lines = new ArrayList<>();
        lines.add(HEADER);
        for (Appointment appointment : storage.values()) {
            lines.add(serialize(appointment));
        }
        lines.addAll(preserved);
        try {
            Files.write(filePath, lines, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to save appointments to file.", ex);
        }
    }

    private String serialize(Appointment appointment) {
        return RECORD_PREFIX + String.join("|",
                encode(appointment.getId()),
                encode(appointment.getAccessCode()),
                encode(appointment.getUser().getId()),
                encode(appointment.getUser().getName()),
                appointment.getType().name(),
                appointment.getTimeSlot().getStart().toString(),
                appointment.getTimeSlot().getEnd().toString(),
                String.valueOf(appointment.getParticipants()),
                appointment.getStatus().name());
    }

    private Appointment deserialize(String line) {
        String payload = line.substring(RECORD_PREFIX.length());
        String[] parts = payload.split("\\|", -1);
        if (parts.length != 8 && parts.length != 9) {
            throw new IllegalStateException("Invalid appointment record in file: " + line);
        }

        String id = decode(parts[0]);
        String accessCode;
        int offset;
        if (parts.length == 9) {
            accessCode = decode(parts[1]);
            offset = 0;
        } else {
            accessCode = "LEGACY-" + Math.abs(id.hashCode() % 1000000);
            offset = -1;
        }

        String userId = decode(parts[2 + offset]);
        String userName = decode(parts[3 + offset]);
        AppointmentType type = AppointmentType.valueOf(parts[4 + offset]);
        LocalDateTime start = LocalDateTime.parse(parts[5 + offset]);
        LocalDateTime end = LocalDateTime.parse(parts[6 + offset]);
        int participants = Integer.parseInt(parts[7 + offset]);
        AppointmentStatus status = AppointmentStatus.valueOf(parts[8 + offset]);

        Appointment appointment = new Appointment(
                id,
                new User(userId, userName),
                accessCode,
                type,
                new TimeSlot(start, end),
                participants);
        if (status == AppointmentStatus.CANCELLED) {
            appointment.cancel();
        }
        return appointment;
    }

    private String encode(String value) {
        return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private String decode(String value) {
        return new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
    }
}
