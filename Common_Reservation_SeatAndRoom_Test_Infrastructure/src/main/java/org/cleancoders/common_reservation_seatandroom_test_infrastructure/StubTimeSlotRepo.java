package org.cleancoders.reservation.usecase;

import org.cleancoders.common_reservation_seatAndRoom.domain.TimeSlot;
import org.cleancoders.common_reservation_seatAndRoom.outbound.TimeSlotRepository;

import java.util.List;
import java.util.Optional;

class StubTimeSlotRepo implements TimeSlotRepository
{
    private final java.util.Map<String, TimeSlot> slots = new java.util.HashMap<>();

    void addTimeSlot(TimeSlot slot)
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
