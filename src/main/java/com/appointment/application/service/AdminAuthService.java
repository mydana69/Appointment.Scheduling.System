package com.appointment.application.service;

import com.appointment.domain.model.Administrator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class AdminAuthService {

    private final Map<String, String> credentials;

    private Administrator loggedInAdmin;

    public AdminAuthService() {
        this.credentials = new HashMap<>();
        credentials.put("admin", "admin123");
    }

    public boolean login(String username, String password) {
        if (credentials.containsKey(username) && credentials.get(username).equals(password)) {
            loggedInAdmin = new Administrator("A-1", username);
            return true;
        }
        return false;
    }

    public void logout() {
        loggedInAdmin = null;
    }

    public boolean isLoggedIn() {
        return loggedInAdmin != null;
    }

    public Optional<Administrator> currentAdmin() {
        return Optional.ofNullable(loggedInAdmin);
    }
}
