package org.cleancoders.web.binder;

import jakarta.inject.Singleton;
import org.cleancoders.infrastructure.persistence.testdata.TestDataReservationRepo;
import org.cleancoders.reservation.outbound.ReservationRepository;
import org.cleancoders.reservation.usecase.*;
import org.cleancoders.web.presenter.WebApiReservationPresenter;
import org.glassfish.hk2.utilities.binding.AbstractBinder;

public class ReservationBinder extends AbstractBinder
{
    @Override
    protected void configure()
    {
        // === UseCases ===
        bind(ReserveUseCase.class).to(ReserveUseCase.class);
        bind(CheckInUseCase.class).to(CheckInUseCase.class);
        bind(CheckOutUseCase.class).to(CheckOutUseCase.class);
        bind(ManageReservationsUseCase.class).to(ManageReservationsUseCase.class);
        bind(ListMyReservationsUseCase.class).to(ListMyReservationsUseCase.class);
        bind(CancelReservationUseCase.class).to(CancelReservationUseCase.class);

        // === Presenters ===
        bind(WebApiReservationPresenter.class)
                .to(ReserveUseCase.Presenter.class)
                .to(CheckInUseCase.Presenter.class)
                .to(CheckOutUseCase.Presenter.class)
                .to(ManageReservationsUseCase.Presenter.class)
                .to(ListMyReservationsUseCase.Presenter.class)
                .to(CancelReservationUseCase.Presenter.class)
                .in(Singleton.class);
        // === Infrastructure ===
        bind(TestDataReservationRepo.class).to(ReservationRepository.class).in(Singleton.class);
    }
}
