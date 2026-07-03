package org.cleancoders.web.binder;

import jakarta.inject.Singleton;
import org.cleancoders.common_reservation_seatAndRoom.outbound.SeatRepository;
import org.cleancoders.common_reservation_seatAndRoom.outbound.TimeSlotRepository;
import org.cleancoders.infrastructure.persistence.testdata.TestDataRoomRepo;
import org.cleancoders.infrastructure.persistence.testdata.TestDataSeatRepo;
import org.cleancoders.infrastructure.persistence.testdata.TestDataTimeSlotRepo;
import org.cleancoders.seatandroom.outbound.RoomRepository;
import org.cleancoders.seatandroom.usecase.*;
import org.cleancoders.web.presenter.WebApiRoomPresenter;
import org.glassfish.hk2.utilities.binding.AbstractBinder;

public class SeatAndRoomBinder extends AbstractBinder
{
    @Override
    protected void configure()
    {
        // === UseCases ===
        bind(ListRoomsUseCase.class).to(ListRoomsUseCase.class);
        bind(ListSeatsUseCase.class).to(ListSeatsUseCase.class);
        bind(ManageRoomsUseCase.class).to(ManageRoomsUseCase.class);
        bind(UpdateRoomUseCase.class).to(UpdateRoomUseCase.class);
        bind(DeleteRoomUseCase.class).to(DeleteRoomUseCase.class);

        // === Presenters ===
        bind(WebApiRoomPresenter.class)
                .to(ListRoomsUseCase.Presenter.class)
                .to(ListSeatsUseCase.Presenter.class)
                .to(ManageRoomsUseCase.Presenter.class)
                .to(UpdateRoomUseCase.Presenter.class)
                .to(DeleteRoomUseCase.Presenter.class)
                .in(Singleton.class);

        // === Infrastructure ===
        bind(TestDataSeatRepo.class).to(SeatRepository.class).in(Singleton.class);
        bind(TestDataTimeSlotRepo.class).to(TimeSlotRepository.class).in(Singleton.class);
        bind(TestDataRoomRepo.class).to(RoomRepository.class).in(Singleton.class);
    }
}
