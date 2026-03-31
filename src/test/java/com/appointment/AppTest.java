package com.appointment;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AppTest {

    @Test
    void mainRunsSuccessfully() {
        assertDoesNotThrow(() -> {
            Path tempDir = Files.createTempDirectory("appointment-app-test-" + UUID.randomUUID());
            System.setProperty("appointment.storage.file", tempDir.resolve("system.db").toString());
            App.main(new String[]{});
        });
    }
}
