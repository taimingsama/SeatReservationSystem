package org.cleancoders.seatandroom.usecase;

import jakarta.inject.Inject;
import org.cleancoders.seatandroom.domain.Seat;
import org.cleancoders.seatandroom.domain.SeatStatus;
import org.cleancoders.seatandroom.domain.StudyRoom;
import org.cleancoders.seatandroom.outbound.ActiveReservationChecker;
import org.cleancoders.seatandroom.outbound.RoomRepository;
import org.cleancoders.seatandroom.outbound.SeatRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * UC-05: 获取某自习室所有座位及状态。
 * <p>
 * 公开用例（不继承 AuthUseCase，无认证要求）。
 * 支持按时间段查询：传入 timeSlotId + date 时，会根据预约情况返回座位在该时段的实际可用状态。
 */
public class ListSeatsUseCase
{

    @Inject
    RoomRepository roomRepo;

    @Inject
    SeatRepository seatRepo;

    @Inject
    ActiveReservationChecker activeReservationChecker;

    @Inject
    Presenter presenter;

    public Output execute(Request request)
    {
        Optional<StudyRoom> room = roomRepo.findById(request.roomId());
        if (room.isEmpty())
        {
            presenter.roomNotFound(request.roomId());
            return new Output(List.of());
        }
        List<Seat> seats = seatRepo.findByRoomId(request.roomId());

        // 如果指定了时间段，根据预约情况计算有效状态
        if (request.timeSlotId() != null && request.date() != null)
        {
            seats = seats.stream()
                    .map(s -> computeEffectiveStatus(s, request.timeSlotId(), request.date()))
                    .toList();
        }

        presenter.presentSeats(room.get(), seats);
        return new Output(seats);
    }

    /**
     * 根据时间段预约情况计算座位的实际状态。
     * 如果座位在当前时段有活跃预约，返回 RESERVED 状态的副本；
     * 否则返回原座位（保持 MAINTENANCE / REMOVED 等静态状态）。
     */
    private Seat computeEffectiveStatus(Seat seat, String timeSlotId, LocalDate date)
    {
        // 非 AVAILABLE 的静态状态（MAINTENANCE / REMOVED）保持不变
        if (seat.status() != SeatStatus.AVAILABLE)
        {
            return seat;
        }

        // 检查该时段是否有活跃预约
        if (activeReservationChecker.isReservedForTimeSlot(
                seat.roomId(), seat.id(), timeSlotId, date))
        {
            return new Seat(seat.id(), seat.roomId(), SeatStatus.RESERVED);
        }

        return seat;
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
    }
}
