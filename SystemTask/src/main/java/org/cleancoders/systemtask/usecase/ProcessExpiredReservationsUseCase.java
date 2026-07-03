package org.cleancoders.systemtask.usecase;

import jakarta.inject.Inject;
import org.cleancoders.reservation.domain.Reservation;
import org.cleancoders.reservation.domain.ReservationStatus;
import org.cleancoders.reservation.outbound.ReservationRepository;
import org.cleancoders.seatandroom.domain.Seat;
import org.cleancoders.seatandroom.domain.TimeSlot;
import org.cleancoders.seatandroom.outbound.SeatRepository;
import org.cleancoders.seatandroom.outbound.TimeSlotRepository;
import org.cleancoders.userandauth.domain.User;
import org.cleancoders.userandauth.outbound.UserRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * 系统定时任务：处理当日已过期的预约。
 * <p>
 * 在时段结束后：
 * <ul>
 *   <li>已签到 (CHECKED_IN) → 自动退座，信用分 +5</li>
 *   <li>未签到 (RESERVED) → 标记为 EXPIRED（超时），信用分 -15</li>
 * </ul>
 */
public class ProcessExpiredReservationsUseCase
{

    @Inject
    ReservationRepository reservationRepo;

    @Inject
    SeatRepository seatRepo;

    @Inject
    TimeSlotRepository timeSlotRepo;

    @Inject
    UserRepository userRepo;

    @Inject
    Presenter presenter;

    /**
     * Returns the current time. Exposed for testability.
     */
    protected LocalDateTime getCurrentTime()
    {
        return LocalDateTime.now();
    }

    public Output execute(Request request)
    {
        LocalDate today = request.date() != null ? request.date() : LocalDate.now();
        LocalDateTime now = getCurrentTime();

        List<Reservation> todayReservations = reservationRepo.findAll().stream()
                .filter(r -> r.date().equals(today))
                .filter(r -> r.status() == ReservationStatus.RESERVED
                        || r.status() == ReservationStatus.CHECKED_IN)
                .toList();

        int autoCheckedOut = 0;
        int expired = 0;

        for (Reservation r : todayReservations)
        {
            var timeSlotOpt = timeSlotRepo.findById(r.timeSlotId());
            if (timeSlotOpt.isEmpty())
            {
                continue;
            }

            TimeSlot timeSlot = timeSlotOpt.get();
            LocalTime slotEnd = LocalTime.parse(timeSlot.endTime());
            LocalDateTime slotEndDateTime = LocalDateTime.of(today, slotEnd);

            // 时段尚未结束，跳过
            if (now.isBefore(slotEndDateTime))
            {
                continue;
            }

            var userOpt = userRepo.findById(r.userId());
            if (userOpt.isEmpty())
            {
                continue;
            }

            User user = userOpt.get();

            if (r.status() == ReservationStatus.CHECKED_IN)
            {
                // 自动退座
                r.checkOut();
                reservationRepo.save(r);

                // 释放座位
                var seatOpt = seatRepo.findByRoomIdAndSeatId(r.roomId(), r.seatId());
                if (seatOpt.isPresent())
                {
                    Seat seat = seatOpt.get();
                    seat.release();
                    seatRepo.save(seat);
                }

                // 信用分 +5（上限 100）
                int newCredit = Math.min(100, user.creditScore() + 5);
                userRepo.save(new User(user.id(), user.username(), user.password(), user.role(),
                        user.name(), user.email(), user.reservationCount(), user.studyHours(),
                        user.checkInCount(), newCredit));

                autoCheckedOut++;
            }
            else if (r.status() == ReservationStatus.RESERVED)
            {
                // 标记为超时
                r.expire();
                reservationRepo.save(r);

                // 信用分 -15（最低 0）
                int newCredit2 = Math.max(0, user.creditScore() - 15);
                userRepo.save(new User(user.id(), user.username(), user.password(), user.role(),
                        user.name(), user.email(), user.reservationCount(), user.studyHours(),
                        user.checkInCount(), newCredit2));

                expired++;
            }
        }

        presenter.onCompleted(today, autoCheckedOut, expired);
        return new Output(today, autoCheckedOut, expired);
    }

    public interface Presenter
    {
        void onCompleted(LocalDate date, int autoCheckedOut, int expired);
    }

    public record Request(LocalDate date)
    {
        public Request()
        {
            this(null);
        }
    }

    public record Output(LocalDate date, int autoCheckedOut, int expired)
    {
    }
}
