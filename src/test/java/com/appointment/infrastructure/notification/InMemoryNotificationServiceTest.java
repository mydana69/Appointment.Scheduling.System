package com.appointment.infrastructure.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.appointment.domain.model.NotificationMessage;
import com.appointment.domain.model.User;
import org.junit.jupiter.api.Test;

class InMemoryNotificationServiceTest {

    @Test
    void sendStoresMessageAndResultIsUnmodifiable() {
        InMemoryNotificationService service = new InMemoryNotificationService();
        NotificationMessage message = new NotificationMessage(new User("U-1", "Maya"), "hello");

        service.send(message);

        assertEquals(1, service.getSentMessages().size());
        assertEquals("hello", service.getSentMessages().get(0).getMessage());
        assertThrows(UnsupportedOperationException.class, () -> service.getSentMessages().add(message));
    }
}
