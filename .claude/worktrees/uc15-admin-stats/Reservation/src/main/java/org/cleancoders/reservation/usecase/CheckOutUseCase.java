package org.cleancoders.reservation.usecase;

import jakarta.inject.Inject;
import org.cleancoders.common.domain.User;
import org.cleancoders.reservation.domain.Reservation;
import org.cleancoders.reservation.domain.ReservationStatus;
import org.cleancoders.common.usecase.AuthUseCase;
import org.cleancoders.common.usecase.StudentAuthUseCase;
import org.cleancoders.common_reservation_seatAndRoom.domain.Seat;
import org.cleancoders.common_reservation_seatAndRoom.domain.TimeSlot;
import org.cleancoders.common_reservation_seatAndRoom.outbound.SeatRepository;
import org.cleancoders.common_reservation_seatAndRoom.outbound.TimeSlotRepository;
import org.cleancoders.reservation.outbound.ReservationRepository;

/**
 * UC-10: 退座。
 * <p>
 * 学生对已签到的座位进行退座操作：
 * <ul>
 *   <li>验证预约属于当前用户</li>
 *   <li>验证预约状态为 CHECKED_IN</li>
 *   <li>预约状态 → CHECKED_OUT</li>
 *   <li>座位状态 → AVAILABLE</li>
 * </ul>
 */
public class CheckOutUseCase extends StudentAuthUseCase<CheckOutUseCase.Request, CheckOutUseCase.Output> {

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

        // 3. Check status is CHECKED_IN
        if (reservation.status() != ReservationStatus.CHECKED_IN) {
            presenter.invalidStatus(reservation.status());
            return null;
        }

        // 4. Perform check-out on reservation
        reservation.checkOut();
        reservationRepo.save(reservation);

        // 5. Release the seat
        var seatOpt = seatRepo.findById(reservation.seatId());
        String seatNumber = "未知";
        if (seatOpt.isPresent()) {
            Seat seat = seatOpt.get();
            seat.release();
            seatRepo.save(seat);
            seatNumber = seat.seatNumber();
        }

        // 6. Look up time slot for response
        var timeSlotOpt = timeSlotRepo.findById(reservation.timeSlotId());
        String timeSlotLabel = timeSlotOpt.map(TimeSlot::label).orElse("未知");

        presenter.success(reservation.id(), seatNumber, timeSlotLabel);
        return new Output(reservation.id());
    }
}
