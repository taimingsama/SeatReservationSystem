package org.cleancoders.seatandroom.usecase;

import jakarta.inject.Inject;
import org.cleancoders.seatandroom.domain.Seat;
import org.cleancoders.seatandroom.domain.SeatStatus;
import org.cleancoders.seatandroom.outbound.SeatRepository;
import org.cleancoders.userandauth.domain.User;
import org.cleancoders.userandauth.usecase.AdminAuthUseCase;
import org.cleancoders.userandauth.usecase.AuthUseCase;

/**
 * UC-07: 更新座位状态(管理员)。仅允许 AVAILABLE ↔ MAINTENANCE 切换。
 * 座位通过 (roomId, seatId) 复合键定位。
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
        var existing = seatRepo.findByRoomIdAndSeatId(req.roomId(), req.seatId());
        if (existing.isEmpty())
        {
            presenter.seatNotFound(req.roomId(), req.seatId());
            return null;
        }

        Seat seat = existing.get();

        if (req.status() == null)
        {
            presenter.invalidStatus(req.roomId(), req.seatId(), null);
            return null;
        }

        SeatStatus target;
        try
        {
            target = SeatStatus.valueOf(req.status());
        }
        catch (IllegalArgumentException e)
        {
            presenter.invalidStatus(req.roomId(), req.seatId(), req.status());
            return null;
        }

        if (target != SeatStatus.AVAILABLE && target != SeatStatus.MAINTENANCE)
        {
            presenter.invalidStatus(req.roomId(), req.seatId(), req.status());
            return null;
        }

        SeatStatus current = seat.status();
        if (target == SeatStatus.MAINTENANCE)
        {
            if (current != SeatStatus.AVAILABLE)
            {
                presenter.invalidStatusTransition(req.roomId(), req.seatId(), current, target);
                return null;
            }
            seat.markMaintenance();
        }
        else // AVAILABLE
        {
            if (current != SeatStatus.MAINTENANCE)
            {
                presenter.invalidStatusTransition(req.roomId(), req.seatId(), current, target);
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

        void seatNotFound(String roomId, int seatId);

        void invalidStatusTransition(String roomId, int seatId, SeatStatus current, SeatStatus target);

        void invalidStatus(String roomId, int seatId, String status);
    }

    public record Request(String token, String roomId, int seatId, String status)
            implements AuthUseCase.Request
    {
    }

    public record Output(int seatId)
    {
    }
}
