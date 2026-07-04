package org.cleancoders.systemtask.usecase;

import jakarta.inject.Inject;
import org.cleancoders.reservation.domain.Reservation;
import org.cleancoders.reservation.domain.ReservationStatus;
import org.cleancoders.reservation.outbound.ReservationRepository;
import org.cleancoders.seatandroom.domain.Seat;
import org.cleancoders.seatandroom.domain.SeatStatus;
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
 * 系统定时任务：每分钟执行一次，处理当日预约的超时与自动退座。
 * <p>
 * 由 {@link org.cleancoders.web.scheduler.ReservationScheduler} 每分钟自动调用。
 * <p>
 * 处理规则：
 * <ul>
 *   <li>已签到 (CHECKED_IN) + 时段已结束 → 自动退座，信用分 +5</li>
 *   <li>未签到 (RESERVED) + 时段已结束 → 标记为 EXPIRED，信用分 -15</li>
 *   <li>未签到 (RESERVED) + 时段开始前预约 + 时段开始后 30 分钟未签到 → 标记为 EXPIRED，信用分 -15</li>
 *   <li>未签到 (RESERVED) + 时段开始后预约 + 创建后 30 分钟未签到 → 标记为 EXPIRED，信用分 -15</li>
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

    /**
     * Returns the current time. Exposed for testability.
     */
    protected LocalDateTime getCurrentTime()
    {
        return LocalDateTime.now();
    }

    public Output execute()
    {
        LocalDate today = LocalDate.now();
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
            LocalTime slotStart = LocalTime.parse(timeSlot.startTime());
            LocalTime slotEnd = LocalTime.parse(timeSlot.endTime());
            LocalDateTime slotStartDateTime = LocalDateTime.of(today, slotStart);
            LocalDateTime slotEndDateTime = LocalDateTime.of(today, slotEnd);

            if (r.status() == ReservationStatus.CHECKED_IN)
            {
                // 时段未结束则跳过
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

                // 自动退座
                r.checkOut();
                reservationRepo.save(r);

                // 释放座位
                var seatOpt = seatRepo.findByRoomIdAndSeatId(r.roomId(), r.seatId());
                if (seatOpt.isPresent())
                {
                    Seat seat = seatOpt.get();
                    if (seat.status() == SeatStatus.OCCUPIED)
                    {
                        seat.release();
                        seatRepo.save(seat);
                    }
                }

                // 计算学习时长：签到时间 ~ 时段结束时间
                int studyHoursToAdd = 0;
                if (r.checkInAt() != null) {
                    studyHoursToAdd = (int) java.time.Duration.between(r.checkInAt(), slotEndDateTime).toHours();
                }

                // 信用分 +5（上限 100），累加学习时长
                int newCredit = Math.min(100, user.creditScore() + 5);
                userRepo.save(new User(user.id(), user.username(), user.password(), user.role(),
                        user.name(), user.email(), user.reservationCount(),
                        user.studyHours() + studyHoursToAdd,
                        user.checkInCount(), newCredit, user.banned()));

                autoCheckedOut++;
            }
            else if (r.status() == ReservationStatus.RESERVED)
            {
                // 判断是否应标记为 EXPIRED
                boolean shouldExpire = false;

                if (!now.isBefore(slotEndDateTime))
                {
                    // 情况A: 时段已结束
                    shouldExpire = true;
                }
                else if (r.createdAt().isBefore(slotStartDateTime)
                        && !now.isBefore(slotStartDateTime.plusMinutes(30)))
                {
                    // 情况B: 时段开始前预约，时段开始后 30 分钟未签到
                    shouldExpire = true;
                }
                else if (!r.createdAt().isBefore(slotStartDateTime)
                        && !now.isBefore(r.createdAt().plusMinutes(30)))
                {
                    // 情况C: 时段开始后预约，创建后 30 分钟未签到
                    shouldExpire = true;
                }

                if (!shouldExpire)
                {
                    continue;
                }

                var userOpt = userRepo.findById(r.userId());
                if (userOpt.isEmpty())
                {
                    continue;
                }
                User user = userOpt.get();

                // 标记为超时
                r.expire();
                reservationRepo.save(r);

                // 信用分 -15（最低 0）
                int newCredit2 = Math.max(0, user.creditScore() - 15);
                userRepo.save(new User(user.id(), user.username(), user.password(), user.role(),
                        user.name(), user.email(), user.reservationCount(), user.studyHours(),
                        user.checkInCount(), newCredit2, user.banned()));

                expired++;
            }
        }

        return new Output(today, autoCheckedOut, expired);
    }

    public record Output(LocalDate date, int autoCheckedOut, int expired)
    {
    }
}
