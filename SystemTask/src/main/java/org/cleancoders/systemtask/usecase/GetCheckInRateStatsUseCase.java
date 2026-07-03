package org.cleancoders.systemtask.usecase;

import jakarta.inject.Inject;
import org.cleancoders.common.domain.User;
import org.cleancoders.common.usecase.AdminAuthUseCase;
import org.cleancoders.common.usecase.AuthUseCase;
import org.cleancoders.reservation.domain.Reservation;
import org.cleancoders.reservation.domain.ReservationStatus;
import org.cleancoders.reservation.outbound.ReservationRepository;

import java.time.LocalDate;
import java.util.List;

/**
 * UC-15: 今日签到率统计（管理员）。
 */
public class GetCheckInRateStatsUseCase
        extends AdminAuthUseCase<GetCheckInRateStatsUseCase.Request, GetCheckInRateStatsUseCase.Output>
{

    @Inject
    Presenter presenter;

    @Inject
    ReservationRepository reservationRepo;

    public interface Presenter
    {
        void presentCheckInRate(LocalDate date, int totalReservations, int checkedIn, double checkInRate);
    }

    public record Request(String token) implements AuthUseCase.Request {}
    public record Output(LocalDate date, int totalReservations, int checkedIn, double checkInRate) {}

    @Override
    protected Output doExecute(User user, Request req)
    {
        LocalDate today = LocalDate.now();

        List<Reservation> todayReservations = reservationRepo.findAll().stream()
                .filter(r -> r.date().equals(today))
                .toList();

        int total = todayReservations.size();
        int checkedIn = (int) todayReservations.stream()
                .filter(r -> r.status() == ReservationStatus.CHECKED_IN
                        || r.status() == ReservationStatus.CHECKED_OUT)
                .count();

        double rate = total == 0 ? 0.0
                : Math.round((double) checkedIn / total * 1000.0) / 1000.0;

        presenter.presentCheckInRate(today, total, checkedIn, rate);
        return new Output(today, total, checkedIn, rate);
    }
}
