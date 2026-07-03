package org.cleancoders.systemtask.usecase;

import jakarta.inject.Inject;
import org.cleancoders.reservation.domain.Reservation;
import org.cleancoders.reservation.domain.ReservationStatus;
import org.cleancoders.reservation.outbound.ReservationRepository;
import org.cleancoders.userandauth.domain.User;
import org.cleancoders.userandauth.usecase.AdminAuthUseCase;
import org.cleancoders.userandauth.usecase.AuthUseCase;

import java.time.LocalDate;
import java.util.List;

/**
 * UC-15: 今日违约率统计（管理员）。
 */
public class GetNoShowRateStatsUseCase
        extends AdminAuthUseCase<GetNoShowRateStatsUseCase.Request, GetNoShowRateStatsUseCase.Output>
{

    @Inject
    Presenter presenter;

    @Inject
    ReservationRepository reservationRepo;

    public interface Presenter
    {
        void presentNoShowRate(LocalDate date, int totalReservations, int expired, double noShowRate);
    }

    public record Request(String token) implements AuthUseCase.Request {}
    public record Output(LocalDate date, int totalReservations, int expired, double noShowRate) {}

    @Override
    protected Output doExecute(User user, Request req)
    {
        LocalDate today = LocalDate.now();

        List<Reservation> todayReservations = reservationRepo.findAll().stream()
                .filter(r -> r.date().equals(today))
                .toList();

        int total = todayReservations.size();
        int expired = (int) todayReservations.stream()
                .filter(r -> r.status() == ReservationStatus.EXPIRED)
                .count();

        double rate = total == 0 ? 0.0
                : Math.round((double) expired / total * 1000.0) / 1000.0;

        presenter.presentNoShowRate(today, total, expired, rate);
        return new Output(today, total, expired, rate);
    }
}
