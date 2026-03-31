package com.appointment.persistence;

import com.appointment.domain.model.Appointment;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryAppointmentRepository implements AppointmentRepository {

    private final Map<String, Appointment> storage;

    public InMemoryAppointmentRepository() {
        this.storage = new ConcurrentHashMap<>();
    }

    @Override
    public void save(Appointment appointment) {
        storage.put(appointment.getId(), appointment);
    }

    @Override
    public Optional<Appointment> findById(String id) {
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public List<Appointment> findAll() {
        return new ArrayList<>(storage.values());
    }
}
