package com.appointment.infrastructure.time;

import com.appointment.application.port.TimeProvider;
import java.time.LocalDateTime;

public class SystemTimeProvider implements TimeProvider {

    @Override
    public LocalDateTime now() {
        return LocalDateTime.now();
    }
}
