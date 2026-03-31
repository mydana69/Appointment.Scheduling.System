package com.appointment.application.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AdminAuthServiceTest {

    @Test
    void loginSucceedsWithValidCredentials() {
        AdminAuthService authService = new AdminAuthService();

        boolean loginResult = authService.login("admin", "admin123");

        assertTrue(loginResult);
        assertTrue(authService.isLoggedIn());
    }

    @Test
    void loginFailsWithInvalidCredentials() {
        AdminAuthService authService = new AdminAuthService();

        boolean loginResult = authService.login("admin", "badPassword");

        assertFalse(loginResult);
        assertFalse(authService.isLoggedIn());
    }

    @Test
    void logoutClosesSession() {
        AdminAuthService authService = new AdminAuthService();
        authService.login("admin", "admin123");

        authService.logout();

        assertFalse(authService.isLoggedIn());
    }
}
