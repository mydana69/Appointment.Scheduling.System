package com.appointment.infrastructure.notification;

import com.appointment.application.port.NotificationService;
import com.appointment.domain.model.NotificationMessage;
import com.appointment.domain.model.User;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class FileNotificationService implements NotificationService {

    private static final String HEADER = "NOTIF_HEADER|recipientIdB64|recipientNameB64|messageB64";

    private static final String RECORD_PREFIX = "NOTIF|";

    private final Path filePath;

    private final List<NotificationMessage> sentMessages;

    public FileNotificationService() {
        this(Paths.get(System.getProperty("appointment.storage.file", "data/system.db")));
    }

    public FileNotificationService(Path filePath) {
        this.filePath = Objects.requireNonNull(filePath, "filePath must not be null");
        this.sentMessages = new ArrayList<>();
        initializeStorageFile();
        loadFromDisk();
    }

    @Override
    public synchronized void send(NotificationMessage message) {
        sentMessages.add(message);
        appendLine(serialize(message));
    }

    public synchronized List<NotificationMessage> getSentMessages() {
        return Collections.unmodifiableList(new ArrayList<>(sentMessages));
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
            throw new IllegalStateException("Failed to initialize notification storage file.", ex);
        }
    }

    private void loadFromDisk() {
        try {
            List<String> lines = Files.readAllLines(filePath, StandardCharsets.UTF_8);
            for (String line : lines) {
                if (!line.startsWith(RECORD_PREFIX)) {
                    continue;
                }
                sentMessages.add(deserialize(line));
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to load notifications from file.", ex);
        }
    }

    private void appendLine(String line) {
        try {
            ensureHeaderPresent();
            Files.write(filePath, (line + System.lineSeparator()).getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to save notification to file.", ex);
        }
    }

    private String serialize(NotificationMessage message) {
        return RECORD_PREFIX + String.join("|",
                encode(message.getRecipient().getId()),
                encode(message.getRecipient().getName()),
                encode(message.getMessage()));
    }

    private NotificationMessage deserialize(String line) {
        String payload = line.substring(RECORD_PREFIX.length());
        String[] parts = payload.split("\\|", -1);
        if (parts.length != 3) {
            throw new IllegalStateException("Invalid notification record in file: " + line);
        }
        User recipient = new User(decode(parts[0]), decode(parts[1]));
        return new NotificationMessage(recipient, decode(parts[2]));
    }

    private void ensureHeaderPresent() {
        try {
            List<String> lines = Files.readAllLines(filePath, StandardCharsets.UTF_8);
            if (!lines.contains(HEADER)) {
                Files.write(filePath, (HEADER + System.lineSeparator()).getBytes(StandardCharsets.UTF_8),
                        StandardOpenOption.CREATE,
                        StandardOpenOption.APPEND);
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to prepare notification header.", ex);
        }
    }

    private String encode(String value) {
        return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private String decode(String value) {
        return new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
    }
}
