package org.cleancoders.reservation.usecase;

import jakarta.inject.Inject;
import org.cleancoders.reservation.domain.Reservation;
import org.cleancoders.reservation.outbound.ReservationRepository;
import org.cleancoders.seatandroom.domain.TimeSlot;
import org.cleancoders.seatandroom.outbound.TimeSlotRepository;
import org.cleancoders.userandauth.domain.User;
import org.cleancoders.userandauth.usecase.AuthUseCase;
import org.cleancoders.userandauth.usecase.StudentAuthUseCase;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * UC-12: 查看我的预约。
 * <p>
 * 学生查看自己的所有预约记录（当前 + 历史），
 * 包含座位和时段信息。
 */
public class ListMyReservationsUseCase extends StudentAuthUseCase<ListMyReservationsUseCase.Request, ListMyReservationsUseCase.Output> {

    @Inject
    protected Presenter presenter;

    @Inject
    protected ReservationRepository reservationRepo;

    @Inject
    protected TimeSlotRepository timeSlotRepo;

    // --- Presenter ---

    public interface Presenter {
        void presentReservations(List<ReservationItem> items);
    }

    // --- Request / Output ---

    public record Request(String token) implements AuthUseCase.Request {
    }

    public record Output(List<ReservationItem> items) {
    }

    /**
     * Value object representing a reservation summary for the response.
     */
    public record ReservationItem(
            String reservationId,
            String roomId,
            int seatId,
            String timeSlotId,
            String timeSlotLabel,
            LocalDate date,
            String status,
            LocalDateTime createdAt
    ) {
    }

    // --- Business Logic ---

    @Override
    protected Output doExecute(User user, Request req) {
        List<Reservation> reservations = reservationRepo.findByUserId(user.id());

        List<ReservationItem> items = reservations.stream()
                .map(r -> {
                    String timeSlotLabel = timeSlotRepo.findById(r.timeSlotId())
                            .map(TimeSlot::label).orElse("未知");

                    return new ReservationItem(
                            r.id(), r.roomId(), r.seatId(),
                            r.timeSlotId(), timeSlotLabel,
                            r.date(), r.status().name(), r.createdAt()
                    );
                })
                .toList();

        presenter.presentReservations(items);
        return new Output(items);
    }
}
