package com.appointment.domain.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Schedule {

    private final List<TimeSlot> slots;

    public Schedule(List<TimeSlot> slots) {
        this.slots = new ArrayList<>(Objects.requireNonNull(slots, "slots must not be null"));
    }

    public List<TimeSlot> getSlots() {
        return Collections.unmodifiableList(slots);
    }
}
