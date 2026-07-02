package org.cleancoders.web.binder;

import jakarta.inject.Singleton;
import org.cleancoders.common.outbound.TokenService;
import org.cleancoders.common.outbound.UserRepository;
import org.cleancoders.common_reservation_seatAndRoom.outbound.SeatRepository;
import org.cleancoders.common_reservation_seatAndRoom.outbound.TimeSlotRepository;
import org.cleancoders.infrastructure.persistence.InMemoryReservationRepo;
import org.cleancoders.infrastructure.persistence.InMemorySeatRepo;
import org.cleancoders.infrastructure.persistence.InMemoryTimeSlotRepo;
import org.cleancoders.infrastructure.persistence.InMemoryUserRepo;
import org.cleancoders.infrastructure.security.BCryptPasswordEncoder;
import org.cleancoders.infrastructure.security.JjwtTokenService;
import org.cleancoders.reservation.outbound.ReservationRepository;
import org.cleancoders.reservation.usecase.CancelReservationUseCase;
import org.cleancoders.reservation.usecase.CheckInUseCase;
import org.cleancoders.reservation.usecase.CheckOutUseCase;
import org.cleancoders.reservation.usecase.ListMyReservationsUseCase;
import org.cleancoders.reservation.usecase.ManageReservationsUseCase;
import org.cleancoders.reservation.usecase.ReserveUseCase;
import org.cleancoders.userandauth.outbound.PasswordEncoder;
import org.cleancoders.userandauth.usecase.GetMeUseCase;
import org.cleancoders.userandauth.usecase.LoginUseCase;
import org.cleancoders.userandauth.usecase.RegisterUseCase;
import org.cleancoders.web.presenter.WebApiAuthPresenter;
import org.cleancoders.web.presenter.WebApiReservationPresenter;
import org.glassfish.hk2.utilities.binding.AbstractBinder;

/**
 * HK2 dependency injection binder.
 * Binds outbound interface implementations from Infrastructure module
 * to their corresponding interfaces defined in business modules.
 * <p>
 * Binding rules:
 * - bind(Implementation.class).to(Interface.class); — create new instance per injection (PerLookup)
 * - bind(instance).to(Contract.class); — share the same instance across contracts
 */
public class AppBinder extends AbstractBinder
{

    @Override
    protected void configure()
    {
        // === UserAndAuth UseCases ===
        bind(LoginUseCase.class).to(LoginUseCase.class);
        bind(RegisterUseCase.class).to(RegisterUseCase.class);
        bind(GetMeUseCase.class).to(GetMeUseCase.class);

        // === Presenters (instance binding: UseCases and AuthResource share same ThreadLocal) ===
        WebApiAuthPresenter presenterInstance = new WebApiAuthPresenter();
        bind(presenterInstance).to(WebApiAuthPresenter.class);
        bind(presenterInstance).to(LoginUseCase.Presenter.class);
        bind(presenterInstance).to(RegisterUseCase.Presenter.class);
        bind(presenterInstance).to(GetMeUseCase.Presenter.class);

        // === Infrastructure → Outbound ===
        bind(InMemoryUserRepo.class).to(UserRepository.class).in(Singleton.class);
        bind(BCryptPasswordEncoder.class).to(PasswordEncoder.class).in(Singleton.class);
        bind(JjwtTokenService.class).to(TokenService.class).in(Singleton.class);

        // === SeatAndRoom ===
        bind(InMemorySeatRepo.class).to(SeatRepository.class).in(Singleton.class);
        bind(InMemoryTimeSlotRepo.class).to(TimeSlotRepository.class).in(Singleton.class);

        // === Reservation UseCases ===
        bind(ReserveUseCase.class).to(ReserveUseCase.class);
        bind(CheckInUseCase.class).to(CheckInUseCase.class);
        bind(CheckOutUseCase.class).to(CheckOutUseCase.class);
        bind(CancelReservationUseCase.class).to(CancelReservationUseCase.class);
        bind(ListMyReservationsUseCase.class).to(ListMyReservationsUseCase.class);
        bind(ManageReservationsUseCase.class).to(ManageReservationsUseCase.class);

        // === Reservation Presenters ===
        WebApiReservationPresenter reservationPresenterInstance = new WebApiReservationPresenter();
        bind(reservationPresenterInstance).to(WebApiReservationPresenter.class);
        bind(reservationPresenterInstance).to(ReserveUseCase.Presenter.class);
        bind(reservationPresenterInstance).to(CheckInUseCase.Presenter.class);
        bind(reservationPresenterInstance).to(CheckOutUseCase.Presenter.class);
        bind(reservationPresenterInstance).to(CancelReservationUseCase.Presenter.class);
        bind(reservationPresenterInstance).to(ListMyReservationsUseCase.Presenter.class);
        bind(reservationPresenterInstance).to(ManageReservationsUseCase.Presenter.class);

        // === Reservation Repositories ===
        bind(InMemoryReservationRepo.class).to(ReservationRepository.class).in(Singleton.class);

        // === SystemTask ===
        // bind(InMemoryTaskRepo.class).to(TaskRepository.class);
    }
}
