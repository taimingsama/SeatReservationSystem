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

import java.time.Duration;
import java.time.LocalTime;

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
public class CheckOutUseCase extends StudentAuthUseCase<CheckOutUseCase.Request, CheckOutUseCase.Output>
{

    @Inject
    protected Presenter presenter;

    @Inject
    protected ReservationRepository reservationRepo;

    @Inject
    protected SeatRepository seatRepo;

    @Inject
    protected TimeSlotRepository timeSlotRepo;

    // --- Presenter ---

    @Override
    protected Output doExecute(User user, Request req)
    {
        // 1. Find reservation
        var reservationOpt = reservationRepo.findById(req.reservationId());
        if (reservationOpt.isEmpty())
        {
            presenter.reservationNotFound(req.reservationId());
            return null;
        }

        Reservation reservation = reservationOpt.get();

        // 2. Check ownership
        if (!reservation.userId().equals(user.id()))
        {
            presenter.notYourReservation();
            return null;
        }

        // 3. Check status is CHECKED_IN
        if (reservation.status() != ReservationStatus.CHECKED_IN)
        {
            presenter.invalidStatus(reservation.status());
            return null;
        }

        // 4. Perform check-out on reservation
        reservation.checkOut();
        reservationRepo.save(reservation);

        // 5. Release the seat
        var seatOpt = seatRepo.findByRoomIdAndSeatId(reservation.roomId(), reservation.seatId());
        String seatNumber = "未知";
        if (seatOpt.isPresent())
        {
            Seat seat = seatOpt.get();
            seat.release();
            seatRepo.save(seat);
            seatNumber = String.valueOf(seat.id());
        }

        // 6. Look up time slot for response and calculate study hours
        var timeSlotOpt = timeSlotRepo.findById(reservation.timeSlotId());
        String timeSlotLabel = timeSlotOpt.map(TimeSlot::label).orElse("未知");
        int studyHoursToAdd = 0;
        if (timeSlotOpt.isPresent())
        {
            LocalTime start = LocalTime.parse(timeSlotOpt.get().startTime());
            LocalTime end = LocalTime.parse(timeSlotOpt.get().endTime());
            studyHoursToAdd = (int) Duration.between(start, end).toHours();
        }

        // 7. Update user stats: studyHours + slot duration
        User updatedUser = new User(
                user.id(), user.username(), user.password(), user.role(), user.name(), user.email(),
                user.reservationCount(), user.studyHours() + studyHoursToAdd,
                user.checkInCount(), user.creditScore());
        userRepo.save(updatedUser);

        presenter.success(reservation.id(), seatNumber, timeSlotLabel);
        return new Output(reservation.id());
    }

    // --- Request / Output ---

    public interface Presenter
    {
        void success(String reservationId, String seatNumber, String timeSlot);

        void reservationNotFound(String reservationId);

        void notYourReservation();

        void invalidStatus(ReservationStatus currentStatus);
    }

    public record Request(String token, String reservationId)
            implements AuthUseCase.Request
    {
    }

    // --- Business Logic ---

    public record Output(String reservationId)
    {
    }
}
