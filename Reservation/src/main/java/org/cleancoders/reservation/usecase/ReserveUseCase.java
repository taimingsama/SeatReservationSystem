package org.cleancoders.reservation.usecase;

import jakarta.inject.Inject;
import org.cleancoders.reservation.domain.Reservation;
import org.cleancoders.reservation.domain.ReservationStatus;
import org.cleancoders.reservation.outbound.ReservationRepository;
import org.cleancoders.seatandroom.domain.SeatStatus;
import org.cleancoders.seatandroom.outbound.SeatRepository;
import org.cleancoders.seatandroom.outbound.TimeSlotRepository;
import org.cleancoders.userandauth.domain.User;
import org.cleancoders.userandauth.usecase.AuthUseCase;
import org.cleancoders.userandauth.usecase.StudentAuthUseCase;

import java.time.LocalDate;
import java.util.Set;

/**
 * UC-08: 创建预约。
 * <p>
 * 学生选择座位 (roomId + seatId) + 时段 + 日期，通过冲突检测后创建预约记录。
 */
public class ReserveUseCase extends StudentAuthUseCase<ReserveUseCase.Request, ReserveUseCase.Output>
{

    private static final Set<ReservationStatus> ACTIVE_STATUSES =
            Set.of(ReservationStatus.RESERVED, ReservationStatus.CHECKED_IN);
    @Inject
    Presenter presenter;
    @Inject
    ReservationRepository reservationRepo;
    @Inject
    SeatRepository seatRepo;
    @Inject
    TimeSlotRepository timeSlotRepo;

    private static final int MAX_ACTIVE_RESERVATIONS = 6;

    @Override
    protected Output doExecute(User user, Request req)
    {
        // 0. Check credit score
        if (user.creditScore() <= 0)
        {
            presenter.creditScoreInsufficient();
            return null;
        }

        // 0b. Check active reservation limit (max 6)
        long activeCount = reservationRepo.findByUserId(user.id()).stream()
                .filter(r -> r.status() == ReservationStatus.RESERVED
                        || r.status() == ReservationStatus.CHECKED_IN)
                .count();
        if (activeCount >= MAX_ACTIVE_RESERVATIONS)
        {
            presenter.maxReservationsReached(MAX_ACTIVE_RESERVATIONS);
            return null;
        }

        // 1. Validate time slot exists
        var timeSlot = timeSlotRepo.findById(req.timeSlotId());
        if (timeSlot.isEmpty())
        {
            presenter.timeSlotNotFound(req.timeSlotId());
            return null;
        }

        // 2. Validate seat exists
        var seatOpt = seatRepo.findByRoomIdAndSeatId(req.roomId(), req.seatId());
        if (seatOpt.isEmpty())
        {
            presenter.seatNotFound(req.roomId(), req.seatId());
            return null;
        }

        var seat = seatOpt.get();

        // 3. Check seat is not in MAINTENANCE
        if (seat.status() == SeatStatus.MAINTENANCE)
        {
            presenter.seatNotAvailable(req.roomId(), req.seatId(), timeSlot.get().label());
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
                req.roomId(), req.seatId(), req.date(), req.timeSlotId(), ACTIVE_STATUSES);
        if (existingSeatReservation.isPresent())
        {
            presenter.seatNotAvailable(req.roomId(), req.seatId(), timeSlot.get().label());
            return null;
        }

        // 6. Create and save the reservation
        Reservation reservation = new Reservation(null, user.id(), req.roomId(), req.seatId(), req.timeSlotId(), req.date());
        Reservation saved = reservationRepo.save(reservation);

        // 7. Update user stats: reservationCount + 1
        User updatedUser = new User(
                user.id(), user.username(), user.password(), user.role(), user.name(), user.email(),
                user.reservationCount() + 1, user.studyHours(), user.checkInCount(), user.creditScore());
        userRepo.save(updatedUser);

        // 8. Present success
        presenter.success(saved.id(), String.valueOf(req.seatId()), timeSlot.get().label());
        return new Output(saved.id());
    }

    public interface Presenter
    {
        void success(String reservationId, String seatNumber, String timeSlot);

        void seatNotAvailable(String roomId, int seatId, String timeSlot);

        void duplicateReservation(String existingId);

        void timeSlotNotFound(String timeSlotId);

        void seatNotFound(String roomId, int seatId);

        void creditScoreInsufficient();

        void maxReservationsReached(int max);
    }

    public record Request(String token, String roomId, int seatId, String timeSlotId, LocalDate date)
            implements AuthUseCase.Request
    {
    }

    public record Output(String reservationId)
    {
    }
}
