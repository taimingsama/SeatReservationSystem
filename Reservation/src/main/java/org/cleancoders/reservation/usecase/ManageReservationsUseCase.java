package org.cleancoders.reservation.usecase;

import jakarta.inject.Inject;
import org.cleancoders.reservation.domain.Reservation;
import org.cleancoders.reservation.outbound.ReservationRepository;
import org.cleancoders.seatandroom.domain.StudyRoom;
import org.cleancoders.seatandroom.domain.TimeSlot;
import org.cleancoders.seatandroom.outbound.RoomRepository;
import org.cleancoders.seatandroom.outbound.TimeSlotRepository;
import org.cleancoders.userandauth.domain.User;
import org.cleancoders.userandauth.usecase.AdminAuthUseCase;
import org.cleancoders.userandauth.usecase.AuthUseCase;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * UC-13: 查看所有预约（管理员）。
 * <p>
 * 管理员查看系统中所有预约记录，包含用户、座位和时段信息。
 */
public class ManageReservationsUseCase extends AdminAuthUseCase<ManageReservationsUseCase.Request, ManageReservationsUseCase.Output> {

    @Inject
    protected Presenter presenter;

    @Inject
    protected ReservationRepository reservationRepo;

    @Inject
    protected TimeSlotRepository timeSlotRepo;

    @Inject
    protected RoomRepository roomRepo;

    // --- Presenter ---

    public interface Presenter
    {
        void presentAllReservations(List<ReservationItem> items);
    }

    // --- Request / Output ---

    public record Request(String token) implements AuthUseCase.Request {
    }

    public record Output(List<ReservationItem> items) {
    }

    public record ReservationItem(
            String reservationId,
            String userId,
            String username,
            String roomId,
            String roomName,
            int seatId,
            String timeSlotId,
            String timeSlotLabel,
            LocalDate date,
            String status,
            LocalDateTime createdAt,
            LocalDateTime checkInAt,
            LocalDateTime checkOutAt
    ) {
    }

    // --- Business Logic ---

    @Override
    protected Output doExecute(User user, Request req) {
        List<Reservation> reservations = reservationRepo.findAll();

        List<ReservationItem> items = reservations.stream()
                .map(r -> {
                    String username = userRepo.findById(r.userId())
                            .map(User::username).orElse("未知");
                    String timeSlotLabel = timeSlotRepo.findById(r.timeSlotId())
                            .map(TimeSlot::label).orElse("未知");

                    String roomName = roomRepo.findById(r.roomId())
                            .map(StudyRoom::name).orElse("未知");

                    return new ReservationItem(
                            r.id(), r.userId(), username,
                            r.roomId(), roomName, r.seatId(),
                            r.timeSlotId(), timeSlotLabel,
                            r.date(), r.status().name(),
                            r.createdAt(), r.checkInAt(), r.checkOutAt()
                    );
                })
                .toList();

        presenter.presentAllReservations(items);
        return new Output(items);
    }
}
