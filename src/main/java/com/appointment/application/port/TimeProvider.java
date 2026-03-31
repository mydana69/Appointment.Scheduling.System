package com.appointment.application.port;

import java.time.LocalDateTime;

public interface TimeProvider {

    LocalDateTime now();
}
