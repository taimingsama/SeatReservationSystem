package org.cleancoders.web.presenter;

import jakarta.inject.Singleton;
import jakarta.ws.rs.core.Response;
import org.cleancoders.reservation.domain.ReservationStatus;
import org.cleancoders.reservation.usecase.CancelReservationUseCase;
import org.cleancoders.reservation.usecase.CheckInUseCase;
import org.cleancoders.reservation.usecase.CheckOutUseCase;
import org.cleancoders.reservation.usecase.ReserveUseCase;

import java.util.Map;

/**
 * WebApi presenter implementation for {@link ReserveUseCase.Presenter},
 * {@link CheckInUseCase.Presenter}, and {@link CheckOutUseCase.Presenter}.
 * <p>
 * Uses {@link ThreadLocal} to store the current request's {@link Response},
 * allowing the singleton presenter to serve concurrent HTTP requests safely.
 */
@Singleton
public class WebApiReservationPresenter implements ReserveUseCase.Presenter, CheckInUseCase.Presenter, CheckOutUseCase.Presenter, CancelReservationUseCase.Presenter {

    private final ThreadLocal<Response> current = new ThreadLocal<>();

    // --- ReserveUseCase.Presenter ---

    @Override
    public void success(String reservationId, String seatNumber, String timeSlot) {
        current.set(Response.status(201).entity(Map.of(
                "reservationId", reservationId,
                "seatNumber", seatNumber,
                "timeSlot", timeSlot
        )).build());
    }

    @Override
    public void seatNotAvailable(String seatId, String timeSlot) {
        current.set(Response.status(409).entity(Map.of(
                "error", "座位已被预约",
                "seatId", seatId,
                "timeSlot", timeSlot
        )).build());
    }

    @Override
    public void duplicateReservation(String existingId) {
        current.set(Response.status(409).entity(Map.of(
                "error", "该时段已有预约",
                "existingReservationId", existingId
        )).build());
    }

    @Override
    public void timeSlotNotFound(String timeSlotId) {
        current.set(Response.status(404).entity(Map.of(
                "error", "时段不存在",
                "timeSlotId", timeSlotId
        )).build());
    }

    @Override
    public void seatNotFound(String seatId) {
        current.set(Response.status(404).entity(Map.of(
                "error", "座位不存在",
                "seatId", seatId
        )).build());
    }

    // --- StudentAuthUseCase.StudentPresenter ---

    @Override
    public void forbidden() {
        current.set(Response.status(403).entity(Map.of(
                "error", "权限不足，仅学生可创建预约"
        )).build());
    }

    // --- AuthUseCase.Presenter ---

    @Override
    public void invalidToken() {
        current.set(Response.status(401).entity(Map.of(
                "error", "Invalid or expired token"
        )).build());
    }

    @Override
    public void userNotFound() {
        current.set(Response.status(404).entity(Map.of(
                "error", "User not found"
        )).build());
    }

    // --- CheckInUseCase.Presenter ---

    @Override
    public void reservationNotFound(String reservationId) {
        current.set(Response.status(404).entity(Map.of(
                "error", "预约不存在",
                "reservationId", reservationId
        )).build());
    }

    @Override
    public void notYourReservation() {
        current.set(Response.status(403).entity(Map.of(
                "error", "只能签到自己的预约"
        )).build());
    }

    @Override
    public void invalidStatus(ReservationStatus currentStatus) {
        current.set(Response.status(409).entity(Map.of(
                "error", "当前状态不允许签到",
                "currentStatus", currentStatus.name()
        )).build());
    }

    @Override
    public void checkInNotAvailable(String reason) {
        current.set(Response.status(409).entity(Map.of(
                "error", reason
        )).build());
    }

    // ---

    public Response getResponse() {
        return current.get();
    }
}
