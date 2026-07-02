package org.cleancoders.web.binder;

import jakarta.inject.Singleton;
import org.cleancoders.infrastructure.persistence.testdata.TestDataReservationRepo;
import org.cleancoders.reservation.outbound.ReservationRepository;
import org.cleancoders.reservation.usecase.CheckInUseCase;
import org.cleancoders.reservation.usecase.ReserveUseCase;
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
        // === Presenters ===
        bind(WebApiReservationPresenter.class)
                .to(ReserveUseCase.Presenter.class)
                .to(CheckInUseCase.Presenter.class)
                .in(Singleton.class);
        // === Infrastructure ===
        bind(TestDataReservationRepo.class).to(ReservationRepository.class).in(Singleton.class);
    }
}
