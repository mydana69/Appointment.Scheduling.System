package com.appointment.domain.model;

import java.util.Objects;

public class User {

    private final String id;

    private final String name;

    public User(String id, String name) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.name = Objects.requireNonNull(name, "name must not be null");
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
