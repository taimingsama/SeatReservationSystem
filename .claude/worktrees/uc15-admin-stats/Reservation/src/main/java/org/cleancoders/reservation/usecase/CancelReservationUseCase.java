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

        // 5. Look up seat and time slot for response
        var seatOpt = seatRepo.findById(reservation.seatId());
        String seatNumber = seatOpt.map(Seat::seatNumber).orElse("未知");

        var timeSlotOpt = timeSlotRepo.findById(reservation.timeSlotId());
        String timeSlotLabel = timeSlotOpt.map(TimeSlot::label).orElse("未知");

        presenter.success(reservation.id(), seatNumber, timeSlotLabel);
        return new Output(reservation.id());
    }
}
