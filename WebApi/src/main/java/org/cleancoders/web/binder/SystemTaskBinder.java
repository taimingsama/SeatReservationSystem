package org.cleancoders.web.binder;

import jakarta.inject.Singleton;
import org.cleancoders.systemtask.usecase.*;
import org.cleancoders.web.presenter.WebApiStatsPresenter;
import org.glassfish.hk2.utilities.binding.AbstractBinder;

public class SystemTaskBinder extends AbstractBinder
{
    @Override
    protected void configure()
    {
        // === UseCases ===
        bind(GetSeatUsageStatsUseCase.class).to(GetSeatUsageStatsUseCase.class);
        bind(GetTimeSlotStatsUseCase.class).to(GetTimeSlotStatsUseCase.class);
        bind(GetPopularRoomsStatsUseCase.class).to(GetPopularRoomsStatsUseCase.class);
        bind(GetCheckInRateStatsUseCase.class).to(GetCheckInRateStatsUseCase.class);
        bind(GetNoShowRateStatsUseCase.class).to(GetNoShowRateStatsUseCase.class);

        // === Presenters ===
        bind(WebApiStatsPresenter.class)
                .to(GetSeatUsageStatsUseCase.Presenter.class)
                .to(GetTimeSlotStatsUseCase.Presenter.class)
                .to(GetPopularRoomsStatsUseCase.Presenter.class)
                .to(GetCheckInRateStatsUseCase.Presenter.class)
                .to(GetNoShowRateStatsUseCase.Presenter.class)
                .in(Singleton.class);
    }
}
