package org.cleancoders.systemtask.usecase;

import jakarta.inject.Inject;
import org.cleancoders.common.domain.User;
import org.cleancoders.common.usecase.AdminAuthUseCase;
import org.cleancoders.common.usecase.AuthUseCase;
import org.cleancoders.common_reservation_seatAndRoom.domain.Seat;
import org.cleancoders.common_reservation_seatAndRoom.domain.SeatStatus;
import org.cleancoders.common_reservation_seatAndRoom.outbound.SeatRepository;
import org.cleancoders.reservation.domain.Reservation;
import org.cleancoders.reservation.domain.ReservationStatus;
import org.cleancoders.reservation.outbound.ReservationRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * UC-15: 今日座位使用率统计（管理员）。
 * <p>
 * 统计当天被占用（RESERVED/OCCUPIED）的座位数占总可用座位（非 REMOVED）的比例。
 */
public class GetSeatUsageStatsUseCase
        extends AdminAuthUseCase<GetSeatUsageStatsUseCase.Request, GetSeatUsageStatsUseCase.Output>
{

    @Inject
    Presenter presenter;

    @Inject
    SeatRepository seatRepo;

    @Inject
    ReservationRepository reservationRepo;

    public interface Presenter
    {
        void presentSeatUsage(LocalDate date, int totalSeats, int usedSeats, double usageRate);
    }

    public record Request(String token) implements AuthUseCase.Request {}
    public record Output(LocalDate date, int totalSeats, int usedSeats, double usageRate) {}

    @Override
    protected Output doExecute(User user, Request req)
    {
        LocalDate today = LocalDate.now();

        List<Seat> allSeats = seatRepo.findAll();
        int totalSeats = (int) allSeats.stream()
                .filter(s -> s.status() != SeatStatus.REMOVED)
                .count();

        List<Reservation> todayReservations = reservationRepo.findAll().stream()
                .filter(r -> r.date().equals(today))
                .toList();

        Set<String> usedSeatIds = todayReservations.stream()
                .filter(r -> r.status() == ReservationStatus.RESERVED
                        || r.status() == ReservationStatus.CHECKED_IN)
                .map(Reservation::seatId)
                .collect(Collectors.toSet());
        int usedSeats = usedSeatIds.size();

        double usageRate = totalSeats == 0 ? 0.0
                : Math.round((double) usedSeats / totalSeats * 1000.0) / 1000.0;

        presenter.presentSeatUsage(today, totalSeats, usedSeats, usageRate);
        return new Output(today, totalSeats, usedSeats, usageRate);
    }
}
