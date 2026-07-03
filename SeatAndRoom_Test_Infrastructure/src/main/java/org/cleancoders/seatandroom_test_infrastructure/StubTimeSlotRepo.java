package org.cleancoders.seatandroom_test_infrastructure;

import org.cleancoders.seatandroom.domain.TimeSlot;
import org.cleancoders.seatandroom.outbound.TimeSlotRepository;

import java.util.List;
import java.util.Optional;

public class StubTimeSlotRepo implements TimeSlotRepository
{
    private final java.util.Map<String, TimeSlot> slots = new java.util.HashMap<>();

    public void addTimeSlot(TimeSlot slot)
    {
        slots.put(slot.id(), slot);
    }

    @Override
    public Optional<TimeSlot> findById(String id)
    {
        return Optional.ofNullable(slots.get(id));
    }

    @Override
    public List<TimeSlot> findAll()
    {
        return List.copyOf(slots.values());
    }
}
