package org.cleancoders.seatandroom.usecase;

import jakarta.inject.Inject;
import org.cleancoders.seatandroom.domain.Seat;
import org.cleancoders.seatandroom.domain.SeatStatus;
import org.cleancoders.seatandroom.domain.StudyRoom;
import org.cleancoders.seatandroom.domain.TimeSlot;
import org.cleancoders.seatandroom.outbound.ActiveReservationChecker;
import org.cleancoders.seatandroom.outbound.RoomRepository;
import org.cleancoders.seatandroom.outbound.SeatRepository;
import org.cleancoders.seatandroom.outbound.TimeSlotRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/**
 * UC-05: 获取某自习室所有座位及状态。
 * <p>
 * 公开用例（不继承 AuthUseCase，无认证要求）。
 * 支持按时间段查询：传入 timeSlotId + date 时，根据预约情况和时段类型计算实际状态：
 * <ul>
 *   <li>当前时段 (slotStart ≤ now < slotEnd)：4 状态 — AVAILABLE / RESERVED / OCCUPIED / MAINTENANCE</li>
 *   <li>未来时段 (now < slotStart)：3 状态 — AVAILABLE / RESERVED / MAINTENANCE（无 OCCUPIED）</li>
 *   <li>过去时段 (slotEnd < now)：返回错误</li>
 * </ul>
 */
public class ListSeatsUseCase
{

    @Inject
    RoomRepository roomRepo;

    @Inject
    SeatRepository seatRepo;

    @Inject
    TimeSlotRepository timeSlotRepo;

    @Inject
    ActiveReservationChecker activeReservationChecker;

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
        Optional<StudyRoom> room = roomRepo.findById(request.roomId());
        if (room.isEmpty())
        {
            presenter.roomNotFound(request.roomId());
            return new Output(List.of());
        }
        List<Seat> seats = seatRepo.findByRoomId(request.roomId());

        // 如果指定了时间段，根据预约情况和时段类型计算有效状态
        if (request.timeSlotId() != null && request.date() != null)
        {
            var timeSlotOpt = timeSlotRepo.findById(request.timeSlotId());
            if (timeSlotOpt.isEmpty())
            {
                presenter.roomNotFound(request.roomId());
                return new Output(List.of());
            }

            TimeSlot timeSlot = timeSlotOpt.get();
            LocalTime slotStart = LocalTime.parse(timeSlot.startTime());
            LocalTime slotEnd = LocalTime.parse(timeSlot.endTime());
            LocalDateTime slotStartDateTime = LocalDateTime.of(request.date(), slotStart);
            LocalDateTime slotEndDateTime = LocalDateTime.of(request.date(), slotEnd);
            LocalDateTime now = getCurrentTime();

            // 过去时段不允许查询
            if (now.isAfter(slotEndDateTime))
            {
                presenter.pastTimeSlot(request.timeSlotId(), request.date());
                return new Output(List.of());
            }

            boolean isCurrent = !now.isBefore(slotStartDateTime) && now.isBefore(slotEndDateTime);

            seats = seats.stream()
                    .map(s -> computeEffectiveStatus(s, request.timeSlotId(), request.date(), isCurrent))
                    .toList();
        }

        presenter.presentSeats(room.get(), seats);
        return new Output(seats);
    }

    /**
     * 根据时间段预约情况计算座位的实际状态。
     * <p>
     * 当前时段：CHECKED_IN → OCCUPIED, RESERVED → RESERVED, 无预约 → AVAILABLE
     * 未来时段：任何活跃预约 → RESERVED（无 OCCUPIED）
     * MAINTENANCE / REMOVED 保持不变。
     * <p>
     * 注意：OCCUPIED/RESERVED 状态可能来自其他时段，需要验证是否属于当前查询时段。
     */
    private Seat computeEffectiveStatus(Seat seat, String timeSlotId, LocalDate date, boolean isCurrent)
    {
        // MAINTENANCE / REMOVED 不受时段影响，直接返回
        if (seat.status() == SeatStatus.MAINTENANCE || seat.status() == SeatStatus.REMOVED)
        {
            return seat;
        }

        // 当前时段：该时段 CHECKED_IN → OCCUPIED
        boolean isCheckedInThisSlot = activeReservationChecker.isCheckedInForTimeSlot(
                seat.roomId(), seat.id(), timeSlotId, date);
        if (isCurrent && isCheckedInThisSlot)
        {
            return new Seat(seat.id(), seat.roomId(), SeatStatus.OCCUPIED);
        }

        // 该时段有活跃预约 → 被预约
        if (activeReservationChecker.isReservedForTimeSlot(
                seat.roomId(), seat.id(), timeSlotId, date))
        {
            return new Seat(seat.id(), seat.roomId(), SeatStatus.RESERVED);
        }

        // 静态 OCCUPIED/RESERVED 但该时段无对应预约 → 视为 AVAILABLE
        return new Seat(seat.id(), seat.roomId(), SeatStatus.AVAILABLE);
    }

    public record Request(String roomId, String timeSlotId, LocalDate date)
    {
        /** 不指定时间段（返回静态状态） */
        public Request(String roomId)
        {
            this(roomId, null, null);
        }
    }

    public record Output(List<Seat> seats)
    {
    }

    public interface Presenter
    {
        void presentSeats(StudyRoom room, List<Seat> seats);

        void roomNotFound(String roomId);

        void pastTimeSlot(String timeSlotId, LocalDate date);
    }
}
