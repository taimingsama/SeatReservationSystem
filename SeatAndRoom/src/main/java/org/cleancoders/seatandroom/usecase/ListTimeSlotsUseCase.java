package org.cleancoders.seatandroom.usecase;

import jakarta.inject.Inject;
import org.cleancoders.seatandroom.domain.TimeSlot;
import org.cleancoders.seatandroom.outbound.TimeSlotRepository;

import java.util.List;

/**
 * 获取所有预设时间段（公开接口，无需认证）。
 */
public class ListTimeSlotsUseCase
{

    @Inject
    TimeSlotRepository timeSlotRepo;

    @Inject
    Presenter presenter;

    public Output execute()
    {
        List<TimeSlot> slots = timeSlotRepo.findAll();
        presenter.presentTimeSlots(slots);
        return new Output(slots);
    }

    public interface Presenter
    {
        void presentTimeSlots(List<TimeSlot> slots);
    }

    public record Output(List<TimeSlot> slots)
    {
    }
}
