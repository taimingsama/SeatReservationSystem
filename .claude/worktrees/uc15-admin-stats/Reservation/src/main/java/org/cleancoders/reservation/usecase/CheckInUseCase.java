package org.cleancoders.reservation.usecase;

import jakarta.inject.Inject;
import org.cleancoders.common.domain.User;
import org.cleancoders.common.usecase.AuthUseCase;
import org.cleancoders.common.usecase.StudentAuthUseCase;
import org.cleancoders.common_reservation_seatAndRoom.domain.Seat;
import org.cleancoders.common_reservation_seatAndRoom.domain.TimeSlot;
import org.cleancoders.common_reservation_seatAndRoom.outbound.SeatRepository;
import org.cleancoders.common_reservation_seatAndRoom.outbound.TimeSlotRepository;
import org.cleancoders.reservation.domain.Reservation;
import org.cleancoders.reservation.domain.ReservationStatus;
import org.cleancoders.reservation.outbound.ReservationRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * UC-09: 签到。
 * <p>
 * 学生对已预约的座位进行签到，验证签到时间窗口：
 * <ul>
 *   <li>当前时间在时段开始前：需在时段开始后 30 分钟内签到</li>
 *   <li>当前时间在时段内：从当前时间起 30 分钟内签到</li>
 *   <li>当前时间已过时段结束：不可签到</li>
 * </ul>
 */
public class CheckInUseCase extends StudentAuthUseCase<CheckInUseCase.Request, CheckInUseCase.Output>
{

    @Inject
    protected Presenter presenter;

    @Inject
    protected ReservationRepository reservationRepo;

    @Inject
    protected TimeSlotRepository timeSlotRepo;

    @Inject
    protected SeatRepository seatRepo;

    /**
     * Returns the current time. Exposed as a protected method for testability.
     */
    protected LocalDateTime getCurrentTime()
    {
        return LocalDateTime.now();
    }

    // --- Request / Output ---

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

        // 3. Check status is RESERVED
        if (reservation.status() != ReservationStatus.RESERVED)
        {
            presenter.invalidStatus(reservation.status());
            return null;
        }

        // 4. Get time slot
        var timeSlotOpt = timeSlotRepo.findById(reservation.timeSlotId());
        if (timeSlotOpt.isEmpty())
        {
            presenter.reservationNotFound(req.reservationId());
            return null;
        }

        TimeSlot timeSlot = timeSlotOpt.get();

        // 5. Validate check-in time window
        LocalTime slotStart = LocalTime.parse(timeSlot.startTime());
        LocalTime slotEnd = LocalTime.parse(timeSlot.endTime());
        LocalDate date = reservation.date();
        LocalDateTime now = getCurrentTime();

        LocalDateTime slotStartDateTime = LocalDateTime.of(date, slotStart);
        LocalDateTime slotEndDateTime = LocalDateTime.of(date, slotEnd);

        if (now.isAfter(slotEndDateTime))
        {
            presenter.checkInNotAvailable("已过时段结束时间，无法签到");
            return null;
        }

        if (now.isBefore(slotStartDateTime))
        {
            // Case 1: before slot — check-in not yet available
            presenter.checkInNotAvailable("时段尚未开始，请在时段开始后 30 分钟内签到");
            return null;
        }
        // Case 2: within slot — window is [now, now+30min], always satisfied

        // 6. Perform check-in
        reservation.checkIn();
        reservationRepo.save(reservation);

        // 7. Look up seat for response
        var seatOpt = seatRepo.findById(reservation.seatId());
        String seatNumber = seatOpt.map(Seat::seatNumber).orElse("未知");

        presenter.success(reservation.id(), seatNumber, timeSlot.label());
        return new Output(reservation.id());
    }

    public interface Presenter
    {
        void success(String reservationId, String seatNumber, String timeSlot);

        void reservationNotFound(String reservationId);

        void notYourReservation();

        void invalidStatus(ReservationStatus currentStatus);

        void checkInNotAvailable(String reason);
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
