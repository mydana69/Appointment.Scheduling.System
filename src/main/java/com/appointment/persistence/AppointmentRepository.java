package com.appointment.persistence;

import com.appointment.domain.model.Appointment;
import java.util.List;
import java.util.Optional;

public interface AppointmentRepository {

    void save(Appointment appointment);

    Optional<Appointment> findById(String id);

    List<Appointment> findAll();
}
