package org.cleancoders.web.presenter;

import jakarta.inject.Singleton;
import jakarta.ws.rs.core.Response;
import org.cleancoders.reservation.domain.ReservationStatus;
import org.cleancoders.reservation.usecase.CancelReservationUseCase;
import org.cleancoders.reservation.usecase.CheckInUseCase;
import org.cleancoders.reservation.usecase.CheckOutUseCase;
import org.cleancoders.reservation.usecase.ListMyReservationsUseCase;
import org.cleancoders.reservation.usecase.ListMyReservationsUseCase.ReservationItem;
import org.cleancoders.reservation.usecase.ManageReservationsUseCase;
import org.cleancoders.reservation.usecase.ReserveUseCase;
import org.cleancoders.web.dto.common.ErrorResponse;
import org.cleancoders.web.dto.reservation.*;

import java.util.List;
import java.util.Map;

/**
 * WebApi presenter implementation for {@link ReserveUseCase.Presenter},
 * {@link CheckInUseCase.Presenter}, and {@link CheckOutUseCase.Presenter}.
 * <p>
 * Uses {@link ThreadLocal} to store the current request's {@link Response},
 * allowing the singleton presenter to serve concurrent HTTP requests safely.
 */
@Singleton
public class WebApiReservationPresenter extends WebApiCommonPresenter implements
        ReserveUseCase.Presenter,
        CheckInUseCase.Presenter,
        CheckOutUseCase.Presenter,
        CancelReservationUseCase.Presenter,
        ListMyReservationsUseCase.Presenter,
        ManageReservationsUseCase.Presenter
{

    // --- ReserveUseCase.Presenter ---

    @Override
    public void success(String reservationId, String seatNumber, String timeSlot)
    {
        current.set(Response.status(201).entity(new ReservationCreatedResponse(
                reservationId, seatNumber, timeSlot)).build());
    }

    @Override
    public void seatNotAvailable(String seatId, String timeSlot)
    {
        current.set(Response.status(409).entity(new SeatConflictResponse(
                "座位已被预约", seatId, timeSlot)).build());
    }

    @Override
    public void duplicateReservation(String existingId)
    {
        current.set(Response.status(409).entity(new DuplicateReservationResponse(
                "该时段已有预约", existingId)).build());
    }

    @Override
    public void timeSlotNotFound(String timeSlotId)
    {
        current.set(Response.status(404).entity(new TimeSlotNotFoundResponse(
                "时段不存在", timeSlotId)).build());
    }

    @Override
    public void seatNotFound(String seatId)
    {
        current.set(Response.status(404).entity(new SeatNotFoundResponse(
                "座位不存在", seatId)).build());
    }

    // --- StudentAuthUseCase.Presenter (override default 403 message) ---

    @Override
    public void forbidden()
    {
        current.set(Response.status(403).entity(new ErrorResponse(
                "权限不足，仅学生可创建预约")).build());
    }

    // --- CheckInUseCase.Presenter ---

    @Override
    public void reservationNotFound(String reservationId)
    {
        current.set(Response.status(404).entity(new ReservationNotFoundResponse(
                "预约不存在", reservationId)).build());
    }

    @Override
    public void notYourReservation()
    {
        current.set(Response.status(403).entity(new ErrorResponse(
                "只能签到自己的预约")).build());
    }

    @Override
    public void seatNotAvailable(String seatId, String timeSlot)
    {
        current.set(Response.status(409).entity(new SeatConflictResponse(
                "座位已被预约", seatId, timeSlot)).build());
    }

    @Override
    public void duplicateReservation(String existingId)
    {
        current.set(Response.status(409).entity(new DuplicateReservationResponse(
                "该时段已有预约", existingId)).build());
    }

    @Override
    public void timeSlotNotFound(String timeSlotId)
    {
        current.set(Response.status(404).entity(new TimeSlotNotFoundResponse(
                "时段不存在", timeSlotId)).build());
    }

    @Override
    public void seatNotFound(String seatId)
    {
        current.set(Response.status(404).entity(new SeatNotFoundResponse(
                "座位不存在", seatId)).build());
    }

    // --- StudentAuthUseCase.Presenter (override default 403 message) ---

    @Override
    public void forbidden()
    {
        current.set(Response.status(403).entity(new ErrorResponse(
                "权限不足，仅学生可创建预约")).build());
    }

    @Override
    public void invalidStatus(ReservationStatus currentStatus)
    {
        current.set(Response.status(409).entity(new InvalidStatusResponse(
                "当前状态不允许签到", currentStatus)).build());
    }

    @Override
    public void checkInNotAvailable(String reason)
    {
        current.set(Response.status(409).entity(new ErrorResponse(reason)).build());
    }
        
    public void reservationNotFound(String reservationId)
    {
        current.set(Response.status(404).entity(new ReservationNotFoundResponse(
                "预约不存在", reservationId)).build());
    }

    @Override
    public void notYourReservation()
    {
        current.set(Response.status(403).entity(new ErrorResponse(
                "只能签到自己的预约")).build());
    }
    // --- ListMyReservationsUseCase.Presenter ---

    @Override
    public void presentReservations(List<ReservationItem> items) {
        var list = items.stream()
                .map(item -> Map.of(
                        "reservationId", item.reservationId(),
                        "seatId", item.seatId(),
                        "seatNumber", item.seatNumber(),
                        "timeSlotId", item.timeSlotId(),
                        "timeSlotLabel", item.timeSlotLabel(),
                        "date", item.date().toString(),
                        "status", item.status(),
                        "createdAt", item.createdAt().toString()
                ))
                .toList();

        current.set(Response.ok(Map.of("reservations", list)).build());
    }
    
    public void invalidStatus(ReservationStatus currentStatus)
    {
        current.set(Response.status(409).entity(new InvalidStatusResponse(
                "当前状态不允许签到", currentStatus)).build());
    }
    // --- ManageReservationsUseCase.Presenter ---

    @Override
    public void presentAllReservations(List<ManageReservationsUseCase.ReservationItem> items) {
        var list = items.stream()
                .map(item -> {
                    var m = new java.util.LinkedHashMap<String, Object>();
                    m.put("reservationId", item.reservationId());
                    m.put("userId", item.userId());
                    m.put("username", item.username());
                    m.put("seatId", item.seatId());
                    m.put("seatNumber", item.seatNumber());
                    m.put("timeSlotId", item.timeSlotId());
                    m.put("timeSlotLabel", item.timeSlotLabel());
                    m.put("date", item.date().toString());
                    m.put("status", item.status());
                    m.put("createdAt", item.createdAt() != null ? item.createdAt().toString() : null);
                    m.put("checkInAt", item.checkInAt() != null ? item.checkInAt().toString() : null);
                    m.put("checkOutAt", item.checkOutAt() != null ? item.checkOutAt().toString() : null);
                    return m;
                })
                .toList();

        current.set(Response.ok(Map.of("reservations", list)).build());
    public void checkInNotAvailable(String reason)
    {
        current.set(Response.status(409).entity(new ErrorResponse(reason)).build());
    }
}
