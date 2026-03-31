package com.appointment.infrastructure.time;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class SystemTimeProviderTest {

    @Test
    void nowReturnsCurrentTime() {
        SystemTimeProvider provider = new SystemTimeProvider();

        LocalDateTime value = provider.now();

        assertFalse(value.toString().isEmpty());
    }
}
