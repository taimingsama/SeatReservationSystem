package org.cleancoders.web.presenter;

import jakarta.inject.Singleton;
import jakarta.ws.rs.core.Response;
import org.cleancoders.reservation.domain.ReservationStatus;
import org.cleancoders.reservation.usecase.CheckInUseCase;
import org.cleancoders.reservation.usecase.ReserveUseCase;
import org.cleancoders.web.dto.DuplicateReservationResponse;
import org.cleancoders.web.dto.ErrorResponse;
import org.cleancoders.web.dto.InvalidStatusResponse;
import org.cleancoders.web.dto.ReservationCreatedResponse;
import org.cleancoders.web.dto.ReservationNotFoundResponse;
import org.cleancoders.web.dto.SeatConflictResponse;
import org.cleancoders.web.dto.SeatNotFoundResponse;
import org.cleancoders.web.dto.TimeSlotNotFoundResponse;

/**
 * WebApi presenter implementation for {@link ReserveUseCase.Presenter}
 * and {@link CheckInUseCase.Presenter}.
 * <p>
 * Uses {@link ThreadLocal} to store the current request's {@link Response},
 * allowing the singleton presenter to serve concurrent HTTP requests safely.
 */
@Singleton
public class WebApiReservationPresenter implements ReserveUseCase.Presenter, CheckInUseCase.Presenter {

    private final ThreadLocal<Response> current = new ThreadLocal<>();

    // --- ReserveUseCase.Presenter ---

    @Override
    public void success(String reservationId, String seatNumber, String timeSlot) {
        current.set(Response.status(201).entity(new ReservationCreatedResponse(
                reservationId, seatNumber, timeSlot)).build());
    }

    @Override
    public void seatNotAvailable(String seatId, String timeSlot) {
        current.set(Response.status(409).entity(new SeatConflictResponse(
                "座位已被预约", seatId, timeSlot)).build());
    }

    @Override
    public void duplicateReservation(String existingId) {
        current.set(Response.status(409).entity(new DuplicateReservationResponse(
                "该时段已有预约", existingId)).build());
    }

    @Override
    public void timeSlotNotFound(String timeSlotId) {
        current.set(Response.status(404).entity(new TimeSlotNotFoundResponse(
                "时段不存在", timeSlotId)).build());
    }

    @Override
    public void seatNotFound(String seatId) {
        current.set(Response.status(404).entity(new SeatNotFoundResponse(
                "座位不存在", seatId)).build());
    }

    // --- StudentAuthUseCase.StudentPresenter ---

    @Override
    public void forbidden() {
        current.set(Response.status(403).entity(new ErrorResponse(
                "权限不足，仅学生可创建预约")).build());
    }

    // --- AuthUseCase.Presenter ---

    @Override
    public void invalidToken() {
        current.set(Response.status(401).entity(new ErrorResponse(
                "Invalid or expired token")).build());
    }

    @Override
    public void userNotFound() {
        current.set(Response.status(404).entity(new ErrorResponse(
                "User not found")).build());
    }

    // --- CheckInUseCase.Presenter ---

    @Override
    public void reservationNotFound(String reservationId) {
        current.set(Response.status(404).entity(new ReservationNotFoundResponse(
                "预约不存在", reservationId)).build());
    }

    @Override
    public void notYourReservation() {
        current.set(Response.status(403).entity(new ErrorResponse(
                "只能签到自己的预约")).build());
    }

    @Override
    public void invalidStatus(ReservationStatus currentStatus) {
        current.set(Response.status(409).entity(new InvalidStatusResponse(
                "当前状态不允许签到", currentStatus)).build());
    }

    @Override
    public void checkInNotAvailable(String reason) {
        current.set(Response.status(409).entity(new ErrorResponse(reason)).build());
    }

    // ---

    public Response getResponse() {
        return current.get();
    }
}
