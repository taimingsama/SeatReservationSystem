package org.cleancoders.systemtask.usecase;

import jakarta.inject.Inject;
import org.cleancoders.reservation.domain.Reservation;
import org.cleancoders.reservation.outbound.ReservationRepository;
import org.cleancoders.seatandroom.domain.Seat;
import org.cleancoders.seatandroom.domain.StudyRoom;
import org.cleancoders.seatandroom.outbound.RoomRepository;
import org.cleancoders.seatandroom.outbound.SeatRepository;
import org.cleancoders.userandauth.domain.User;
import org.cleancoders.userandauth.usecase.AdminAuthUseCase;
import org.cleancoders.userandauth.usecase.AuthUseCase;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * UC-15: 今日热门自习室排名（管理员）。
 */
public class GetPopularRoomsStatsUseCase
        extends AdminAuthUseCase<GetPopularRoomsStatsUseCase.Request, GetPopularRoomsStatsUseCase.Output>
{

    @Inject
    Presenter presenter;

    @Inject
    RoomRepository roomRepo;

    @Inject
    SeatRepository seatRepo;

    @Inject
    ReservationRepository reservationRepo;

    public record PopularRoomItem(String roomId, String roomName, long reservationCount) {}

    public interface Presenter
    {
        void presentPopularRooms(LocalDate date, List<PopularRoomItem> items);
    }

    public record Request(String token) implements AuthUseCase.Request {}
    public record Output(LocalDate date, List<PopularRoomItem> items) {}

    @Override
    protected Output doExecute(User user, Request req)
    {
        LocalDate today = LocalDate.now();

        Map<Integer, String> seatToRoom = seatRepo.findAll().stream()
                .collect(Collectors.toMap(s -> s.id(), Seat::roomId));

        List<Reservation> todayReservations = reservationRepo.findAll().stream()
                .filter(r -> r.date().equals(today))
                .toList();

        Map<String, Long> countsByRoom = todayReservations.stream()
                .map(r -> seatToRoom.get(r.seatId()))
                .filter(roomId -> roomId != null)
                .collect(Collectors.groupingBy(roomId -> roomId, Collectors.counting()));

        Map<String, String> roomNames = roomRepo.findAll().stream()
                .collect(Collectors.toMap(StudyRoom::id, StudyRoom::name));

        List<PopularRoomItem> items = countsByRoom.entrySet().stream()
                .map(e -> new PopularRoomItem(e.getKey(),
                        roomNames.getOrDefault(e.getKey(), "未知"),
                        e.getValue()))
                .sorted(Comparator.comparingLong(PopularRoomItem::reservationCount).reversed())
                .toList();

        presenter.presentPopularRooms(today, items);
        return new Output(today, items);
    }
}
