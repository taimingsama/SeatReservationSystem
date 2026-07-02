package org.cleancoders.web.binder;

import jakarta.inject.Singleton;
import org.cleancoders.seatandroom.usecase.DeleteRoomUseCase;
import org.cleancoders.seatandroom.usecase.ManageRoomsUseCase;
import org.cleancoders.seatandroom.usecase.UpdateRoomUseCase;
import org.cleancoders.web.presenter.WebApiAdminPresenter;
import org.glassfish.hk2.utilities.binding.AbstractBinder;

public class AdminBinder extends AbstractBinder
{
    @Override
    protected void configure()
    {
        // === UseCases ===
        bind(ManageRoomsUseCase.class).to(ManageRoomsUseCase.class);
        bind(UpdateRoomUseCase.class).to(UpdateRoomUseCase.class);
        bind(DeleteRoomUseCase.class).to(DeleteRoomUseCase.class);

        // === Presenters ===
        bind(WebApiAdminPresenter.class)
                .to(ManageRoomsUseCase.Presenter.class)
                .to(UpdateRoomUseCase.Presenter.class)
                .to(DeleteRoomUseCase.Presenter.class)
                .in(Singleton.class);
    }
}
