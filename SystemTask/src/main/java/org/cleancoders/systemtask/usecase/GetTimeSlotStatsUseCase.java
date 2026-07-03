package org.cleancoders.systemtask.usecase;

import jakarta.inject.Inject;
import org.cleancoders.common.domain.User;
import org.cleancoders.common.usecase.AdminAuthUseCase;
import org.cleancoders.common.usecase.AuthUseCase;
import org.cleancoders.common_reservation_seatAndRoom.domain.TimeSlot;
import org.cleancoders.common_reservation_seatAndRoom.outbound.TimeSlotRepository;
import org.cleancoders.reservation.domain.Reservation;
import org.cleancoders.reservation.outbound.ReservationRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * UC-15: 今日各时段预约量统计（管理员）。
 */
public class GetTimeSlotStatsUseCase
        extends AdminAuthUseCase<GetTimeSlotStatsUseCase.Request, GetTimeSlotStatsUseCase.Output>
{

    @Inject
    Presenter presenter;

    @Inject
    TimeSlotRepository timeSlotRepo;

    @Inject
    ReservationRepository reservationRepo;

    public record TimeSlotStatItem(String timeSlotId, String label, long count) {}

    public interface Presenter
    {
        void presentTimeSlotStats(LocalDate date, List<TimeSlotStatItem> items);
    }

    public record Request(String token) implements AuthUseCase.Request {}
    public record Output(LocalDate date, List<TimeSlotStatItem> items) {}

    @Override
    protected Output doExecute(User user, Request req)
    {
        LocalDate today = LocalDate.now();

        List<Reservation> todayReservations = reservationRepo.findAll().stream()
                .filter(r -> r.date().equals(today))
                .toList();

        Map<String, Long> countsBySlot = todayReservations.stream()
                .collect(Collectors.groupingBy(Reservation::timeSlotId, Collectors.counting()));

        List<TimeSlotStatItem> items = timeSlotRepo.findAll().stream()
                .map(ts -> new TimeSlotStatItem(
                        ts.id(),
                        ts.label(),
                        countsBySlot.getOrDefault(ts.id(), 0L)))
                .toList();

        presenter.presentTimeSlotStats(today, items);
        return new Output(today, items);
    }
}
