package com.appointment.infrastructure.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.appointment.domain.model.NotificationMessage;
import com.appointment.domain.model.User;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import org.junit.jupiter.api.Test;

class FileNotificationServiceTest {

    @Test
    void sendPersistsNotificationAndHeader() throws Exception {
        Path tempDir = Files.createTempDirectory("notif-file-test-");
        Path dbFile = tempDir.resolve("system.db");

        FileNotificationService service = new FileNotificationService(dbFile);
        service.send(new NotificationMessage(new User("U-1", "Ali"), "hello reminder"));

        assertEquals(1, service.getSentMessages().size());
        String fileContent = new String(Files.readAllBytes(dbFile), StandardCharsets.UTF_8);
        assertTrue(fileContent.contains("NOTIF_HEADER|recipientIdB64|recipientNameB64|messageB64"));
        assertTrue(fileContent.contains("NOTIF|"));
    }

    @Test
    void constructorLoadsExistingMessages() throws Exception {
        Path tempDir = Files.createTempDirectory("notif-file-load-");
        Path dbFile = tempDir.resolve("system.db");

        String header = "NOTIF_HEADER|recipientIdB64|recipientNameB64|messageB64";
        String line = "NOTIF|" + encode("U-9") + "|" + encode("Maya") + "|" + encode("scheduled update");
        Files.write(dbFile, (header + System.lineSeparator() + line + System.lineSeparator()).getBytes(StandardCharsets.UTF_8));

        FileNotificationService service = new FileNotificationService(dbFile);

        assertEquals(1, service.getSentMessages().size());
        assertEquals("U-9", service.getSentMessages().get(0).getRecipient().getId());
        assertEquals("scheduled update", service.getSentMessages().get(0).getMessage());
    }

    @Test
    void malformedNotificationRecordThrows() throws Exception {
        Path tempDir = Files.createTempDirectory("notif-file-bad-");
        Path dbFile = tempDir.resolve("system.db");
        Files.write(dbFile, ("NOTIF|BROKEN" + System.lineSeparator()).getBytes(StandardCharsets.UTF_8));

        assertThrows(IllegalStateException.class, () -> new FileNotificationService(dbFile));
    }

    private static String encode(String value) {
        return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }
}
