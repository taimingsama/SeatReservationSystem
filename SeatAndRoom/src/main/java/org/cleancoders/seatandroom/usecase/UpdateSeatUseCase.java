package org.cleancoders.seatandroom.usecase;

import jakarta.inject.Inject;
import org.cleancoders.seatandroom.domain.Seat;
import org.cleancoders.seatandroom.domain.SeatStatus;
import org.cleancoders.seatandroom.outbound.SeatRepository;
import org.cleancoders.userandauth.domain.User;
import org.cleancoders.userandauth.usecase.AdminAuthUseCase;
import org.cleancoders.userandauth.usecase.AuthUseCase;

/**
 * UC-07: 更新座位状态(管理员)。仅允许 AVAILABLE ↔ MAINTENANCE 切换,
 * 复用 {@link Seat#markMaintenance()} / {@link Seat#markAvailable()} 域方法。
 */
public class UpdateSeatUseCase extends AdminAuthUseCase<UpdateSeatUseCase.Request, UpdateSeatUseCase.Output>
{
    @Inject
    Presenter presenter;

    @Inject
    SeatRepository seatRepo;

    @Override
    protected Output doExecute(User user, Request req)
    {
        var existing = seatRepo.findById(req.seatId());
        if (existing.isEmpty())
        {
            presenter.seatNotFound(req.seatId());
            return null;
        }

        Seat seat = existing.get();

        if (req.status() == null)
        {
            presenter.invalidStatus(req.seatId(), null);
            return null;
        }

        SeatStatus target;
        try
        {
            target = SeatStatus.valueOf(req.status());
        }
        catch (IllegalArgumentException e)
        {
            presenter.invalidStatus(req.seatId(), req.status());
            return null;
        }

        // 仅允许管理员可控两态;RESERVED/OCCUPIED 由预约流程流转
        if (target != SeatStatus.AVAILABLE && target != SeatStatus.MAINTENANCE)
        {
            presenter.invalidStatus(req.seatId(), req.status());
            return null;
        }

        // 不靠异常控流:先检查当前状态,只调合法路径
        SeatStatus current = seat.status();
        if (target == SeatStatus.MAINTENANCE)
        {
            if (current != SeatStatus.AVAILABLE)
            {
                presenter.invalidStatusTransition(req.seatId(), current, target);
                return null;
            }
            seat.markMaintenance();
        }
        else // AVAILABLE
        {
            if (current != SeatStatus.MAINTENANCE)
            {
                presenter.invalidStatusTransition(req.seatId(), current, target);
                return null;
            }
            seat.markAvailable();
        }

        Seat saved = seatRepo.save(seat);
        presenter.updateSuccess(saved);
        return new Output(saved.id());
    }

    public interface Presenter
    {
        void updateSuccess(Seat seat);

        void seatNotFound(String seatId);

        void invalidStatusTransition(String seatId, SeatStatus current, SeatStatus target);

        void invalidStatus(String seatId, String status);
    }

    public record Request(String token, String seatId, String status)
            implements AuthUseCase.Request
    {
    }

    public record Output(String seatId)
    {
    }
}