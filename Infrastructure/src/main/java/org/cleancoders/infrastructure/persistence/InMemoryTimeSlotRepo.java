package org.cleancoders.infrastructure.persistence;

import jakarta.inject.Singleton;
import org.cleancoders.seatandroom.domain.TimeSlot;
import org.cleancoders.seatandroom.outbound.TimeSlotRepository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class InMemoryTimeSlotRepo implements TimeSlotRepository {

    private final Map<String, TimeSlot> store = new ConcurrentHashMap<>();

    public InMemoryTimeSlotRepo() {
        // Pre-seed 3 standard time slots
        store.put("ts-1", new TimeSlot("ts-1", "08:00", "12:00", "上午 08:00-12:00"));
        store.put("ts-2", new TimeSlot("ts-2", "13:00", "17:00", "下午 13:00-17:00"));
        store.put("ts-3", new TimeSlot("ts-3", "18:00", "22:00", "晚上 18:00-22:00"));
    }

    @Override
    public Optional<TimeSlot> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<TimeSlot> findAll() {
        return List.copyOf(store.values());
    }
}
