package org.cleancoders.reservation.usecase;

import jakarta.inject.Inject;
import org.cleancoders.common.domain.User;
import org.cleancoders.reservation.domain.Reservation;
import org.cleancoders.reservation.domain.ReservationStatus;
import org.cleancoders.reservation.outbound.ReservationRepository;
import org.cleancoders.seatandroom.domain.SeatStatus;
import org.cleancoders.seatandroom.outbound.SeatRepository;
import org.cleancoders.seatandroom.outbound.TimeSlotRepository;
import org.cleancoders.userandauth.usecase.AuthUseCase;
import org.cleancoders.userandauth.usecase.StudentAuthUseCase;

import java.time.LocalDate;
import java.util.Set;

/**
 * UC-08: 创建预约。
 * <p>
 * 学生选择座位 + 时段 + 日期，通过冲突检测后创建预约记录。
 * <p>
 * 冲突检测（三项检查）：
 * <ol>
 *   <li>座位是否存在且非维护状态</li>
 *   <li>同一用户在同一日期+时段是否已有预约（一人一座）</li>
 *   <li>座位在同一日期+时段是否已被他人预约</li>
 * </ol>
 */
public class ReserveUseCase extends StudentAuthUseCase<ReserveUseCase.Request, ReserveUseCase.Output>
{

    @Inject
    Presenter presenter;

    @Inject
    ReservationRepository reservationRepo;

    @Inject
    SeatRepository seatRepo;

    @Inject
    TimeSlotRepository timeSlotRepo;

    private static final Set<ReservationStatus> ACTIVE_STATUSES =
            Set.of(ReservationStatus.RESERVED, ReservationStatus.CHECKED_IN);

    // --- Presenter ---

    /**
     * Presenter interface for UC-08 output branches.
     * Extends {@link StudentAuthUseCase.StudentPresenter} to inherit auth error branches.
     */
    public interface Presenter extends StudentAuthUseCase.StudentPresenter
    {
        void success(String reservationId, String seatNumber, String timeSlot);

        void seatNotAvailable(String seatId, String timeSlot);

        void duplicateReservation(String existingId);

        void timeSlotNotFound(String timeSlotId);

        void seatNotFound(String seatId);
    }

    @Override
    protected StudentPresenter getPresenter()
    {
        return presenter;
    }

    // --- Request / Output ---

    public record Request(String token, String seatId, String timeSlotId, LocalDate date)
            implements AuthUseCase.AuthRequest
    {
    }

    public record Output(String reservationId)
    {
    }

    // --- Business Logic ---

    @Override
    protected Output doExecute(User user, Request req)
    {
        // 1. Validate time slot exists
        var timeSlot = timeSlotRepo.findById(req.timeSlotId());
        if (timeSlot.isEmpty())
        {
            presenter.timeSlotNotFound(req.timeSlotId());
            return null;
        }

        // 2. Validate seat exists
        var seatOpt = seatRepo.findById(req.seatId());
        if (seatOpt.isEmpty())
        {
            presenter.seatNotFound(req.seatId());
            return null;
        }

        var seat = seatOpt.get();

        // 3. Check seat is not in MAINTENANCE
        if (seat.status() == SeatStatus.MAINTENANCE)
        {
            presenter.seatNotAvailable(req.seatId(), timeSlot.get().label());
            return null;
        }

        // 4. Check user doesn't already have a reservation for same date+timeslot
        var existingUserReservation = reservationRepo.findByUserIdAndDateAndTimeSlotIdAndStatusIn(
                user.id(), req.date(), req.timeSlotId(), ACTIVE_STATUSES);
        if (existingUserReservation.isPresent())
        {
            presenter.duplicateReservation(existingUserReservation.get().id());
            return null;
        }

        // 5. Check seat not already reserved for same date+timeslot
        var existingSeatReservation = reservationRepo.findBySeatIdAndDateAndTimeSlotIdAndStatusIn(
                req.seatId(), req.date(), req.timeSlotId(), ACTIVE_STATUSES);
        if (existingSeatReservation.isPresent())
        {
            presenter.seatNotAvailable(req.seatId(), timeSlot.get().label());
            return null;
        }

        // 6. Create and save the reservation
        Reservation reservation = new Reservation(null, user.id(), req.seatId(), req.timeSlotId(), req.date());
        Reservation saved = reservationRepo.save(reservation);

        // 7. Present success
        presenter.success(saved.id(), seat.seatNumber(), timeSlot.get().label());
        return new Output(saved.id());
    }
}
