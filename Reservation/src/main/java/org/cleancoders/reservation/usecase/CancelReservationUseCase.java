package org.cleancoders.reservation.usecase;

import jakarta.inject.Inject;
import org.cleancoders.reservation.domain.Reservation;
import org.cleancoders.reservation.domain.ReservationStatus;
import org.cleancoders.reservation.outbound.ReservationRepository;
import org.cleancoders.seatandroom.domain.Seat;
import org.cleancoders.seatandroom.domain.TimeSlot;
import org.cleancoders.seatandroom.outbound.SeatRepository;
import org.cleancoders.seatandroom.outbound.TimeSlotRepository;
import org.cleancoders.userandauth.domain.User;
import org.cleancoders.userandauth.usecase.AuthUseCase;
import org.cleancoders.userandauth.usecase.StudentAuthUseCase;

/**
 * UC-11: 取消预约。
 * <p>
 * 学生对未签到的预约进行取消操作：
 * <ul>
 *   <li>验证预约属于当前用户</li>
 *   <li>验证预约状态为 RESERVED（已签到或已退座不可取消）</li>
 *   <li>预约状态 → CANCELLED</li>
 * </ul>
 */
public class CancelReservationUseCase extends StudentAuthUseCase<CancelReservationUseCase.Request, CancelReservationUseCase.Output> {

    @Inject
    protected Presenter presenter;

    @Inject
    protected ReservationRepository reservationRepo;

    @Inject
    protected SeatRepository seatRepo;

    @Inject
    protected TimeSlotRepository timeSlotRepo;

    // --- Presenter ---

    public interface Presenter {
        void success(String reservationId, String seatNumber, String timeSlot);

        void reservationNotFound(String reservationId);

        void notYourReservation();

        void invalidStatus(ReservationStatus currentStatus);
    }

    // --- Request / Output ---

    public record Request(String token, String reservationId)
            implements AuthUseCase.Request {
    }

    public record Output(String reservationId) {
    }

    // --- Business Logic ---

    @Override
    protected Output doExecute(User user, Request req) {
        // 1. Find reservation
        var reservationOpt = reservationRepo.findById(req.reservationId());
        if (reservationOpt.isEmpty()) {
            presenter.reservationNotFound(req.reservationId());
            return null;
        }

        Reservation reservation = reservationOpt.get();

        // 2. Check ownership
        if (!reservation.userId().equals(user.id())) {
            presenter.notYourReservation();
            return null;
        }

        // 3. Check status is RESERVED
        if (reservation.status() != ReservationStatus.RESERVED) {
            presenter.invalidStatus(reservation.status());
            return null;
        }

        // 4. Cancel the reservation
        reservation.cancel();
        reservationRepo.save(reservation);

        // 5. Update user stats: creditScore - 5 (min 0)
        int newCreditScore = Math.max(0, user.creditScore() - 5);
        User updatedUser = new User(
                user.id(), user.username(), user.password(), user.role(), user.name(), user.email(),
                user.reservationCount(), user.studySeconds(), user.checkInCount(), newCreditScore, user.banned());
        userRepo.save(updatedUser);

        // 6. Look up seat and time slot for response
        var seatOpt = seatRepo.findByRoomIdAndSeatId(reservation.roomId(), reservation.seatId());
        String seatNumber = seatOpt.map(s -> String.valueOf(s.id())).orElse("未知");

        var timeSlotOpt = timeSlotRepo.findById(reservation.timeSlotId());
        String timeSlotLabel = timeSlotOpt.map(TimeSlot::label).orElse("未知");

        presenter.success(reservation.id(), seatNumber, timeSlotLabel);
        return new Output(reservation.id());
    }
}
