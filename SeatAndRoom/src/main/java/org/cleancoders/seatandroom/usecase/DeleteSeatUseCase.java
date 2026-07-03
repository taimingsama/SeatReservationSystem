package org.cleancoders.seatandroom.usecase;

import jakarta.inject.Inject;
import org.cleancoders.common.domain.User;
import org.cleancoders.common.usecase.AdminAuthUseCase;
import org.cleancoders.common.usecase.AuthUseCase;
import org.cleancoders.common_reservation_seatAndRoom.domain.Seat;
import org.cleancoders.common_reservation_seatAndRoom.domain.SeatStatus;
import org.cleancoders.common_reservation_seatAndRoom.outbound.ActiveReservationChecker;
import org.cleancoders.common_reservation_seatAndRoom.outbound.SeatRepository;

/**
 * UC-07: 删除座位(管理员,软删除)。
 * <p>
 * 将可用或维护中的座位标记为 REMOVED。RESERVED/OCCUPIED 的座位或有
 * 活跃预约(RESERVED/CHECKED_IN)的座位拒绝删除,返回 409。
 */
public class DeleteSeatUseCase extends AdminAuthUseCase<DeleteSeatUseCase.Request, DeleteSeatUseCase.Output>
{

    @Inject
    Presenter presenter;

    @Inject
    SeatRepository seatRepo;

    @Inject
    ActiveReservationChecker activeReservationChecker;

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
        if (seat.status() == SeatStatus.REMOVED)
        {
            presenter.seatAlreadyRemoved(req.seatId());
            return null;
        }

        // 域级保护: RESERVED/OCCUPIED 不得删除
        if (seat.status() != SeatStatus.AVAILABLE
                && seat.status() != SeatStatus.MAINTENANCE)
        {
            presenter.seatInUse(req.seatId(), seat.status());
            return null;
        }

        // 活跃预约检查: 查预约表, 有 RESERVED/CHECKED_IN 则拒绝
        if (activeReservationChecker.hasActiveForSeat(req.seatId()))
        {
            presenter.seatHasActiveReservations(req.seatId());
            return null;
        }

        seat.markRemoved();
        Seat saved = seatRepo.save(seat);
        presenter.deleteSuccess(saved.id());
        return new Output(saved.id());
    }

    public interface Presenter
    {
        void deleteSuccess(String seatId);

        void seatNotFound(String seatId);

        void seatAlreadyRemoved(String seatId);

        void seatInUse(String seatId, SeatStatus current);

        void seatHasActiveReservations(String seatId);
    }

    public record Request(String token, String seatId)
            implements AuthUseCase.Request
    {
    }

    public record Output(String seatId)
    {
    }
}
