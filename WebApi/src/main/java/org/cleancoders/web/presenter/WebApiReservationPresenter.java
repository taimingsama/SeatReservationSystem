package org.cleancoders.web.presenter;

import jakarta.inject.Singleton;
import jakarta.ws.rs.core.Response;
import org.cleancoders.reservation.domain.ReservationStatus;
import org.cleancoders.reservation.usecase.*;
import org.cleancoders.reservation.usecase.ListMyReservationsUseCase.ReservationItem;
import org.cleancoders.web.dto.common.ErrorResponse;
import org.cleancoders.web.dto.reservation.*;

import java.util.List;

/**
 * WebApi presenter implementation for all reservation-related use cases.
 */
@Singleton
public class WebApiReservationPresenter extends WebApiPresenter implements
        ReserveUseCase.Presenter,
        CheckInUseCase.Presenter,
        CheckOutUseCase.Presenter,
        CancelReservationUseCase.Presenter,
        ListMyReservationsUseCase.Presenter,
        ManageReservationsUseCase.Presenter
{

    // -------------------------------------------------------
    // Shared success method (Reserve / CheckIn / CheckOut / Cancel)
    // -------------------------------------------------------

    @Override
    public void success(String reservationId, String seatNumber, String timeSlot)
    {
        responseContext.set(Response.status(201).entity(new ReservationCreatedResponse(
                reservationId, seatNumber, timeSlot)).build());
    }

    // -------------------------------------------------------
    // ReserveUseCase.Presenter
    // -------------------------------------------------------

    @Override
    public void seatNotAvailable(String roomId, int seatId, String timeSlot)
    {
        responseContext.set(Response.status(409).entity(new SeatConflictResponse(
                "座位已被预约", roomId + ":" + seatId, timeSlot)).build());
    }

    @Override
    public void duplicateReservation(String existingId)
    {
        responseContext.set(Response.status(409).entity(new DuplicateReservationResponse(
                "该时段已有预约", existingId)).build());
    }

    @Override
    public void timeSlotNotFound(String timeSlotId)
    {
        responseContext.set(Response.status(404).entity(new TimeSlotNotFoundResponse(
                "时段不存在", timeSlotId)).build());
    }

    @Override
    public void seatNotFound(String roomId, int seatId)
    {
        responseContext.set(Response.status(404).entity(new SeatNotFoundResponse(
                "座位不存在", roomId + ":" + seatId)).build());
    }

    // --- CheckInUseCase.Presenter ---

    @Override
    public void reservationNotFound(String reservationId)
    {
        responseContext.set(Response.status(404).entity(new ReservationNotFoundResponse(
                "预约不存在", reservationId)).build());
    }

    @Override
    public void notYourReservation()
    {
        responseContext.set(Response.status(403).entity(new ErrorResponse(
                "只能签到自己的预约")).build());
    }

    @Override
    public void invalidStatus(ReservationStatus currentStatus)
    {
        responseContext.set(Response.status(409).entity(new InvalidStatusResponse(
                "当前状态不允许签到", currentStatus)).build());
    }

    @Override
    public void checkInNotAvailable(String reason)
    {
        responseContext.set(Response.status(409).entity(new ErrorResponse(reason)).build());
    }

    // --- ListMyReservationsUseCase.Presenter ---

    @Override
    public void presentReservations(List<ReservationItem> items)
    {
        List<ReservationItemResponse> dtos = items.stream()
                .map(item -> new ReservationItemResponse(
                        item.reservationId(),
                        item.roomId(),
                        item.seatId(),
                        item.timeSlotId(),
                        item.timeSlotLabel(),
                        item.date().toString(),
                        item.status(),
                        item.createdAt().toString()
                ))
                .toList();

        responseContext.set(Response.ok(new ReservationListResponse(dtos)).build());
    }

    // --- ManageReservationsUseCase.Presenter ---

    @Override
    public void presentAllReservations(List<ManageReservationsUseCase.ReservationItem> items)
    {
        List<AdminReservationItemResponse> dtos = items.stream()
                .map(item -> new AdminReservationItemResponse(
                        item.reservationId(),
                        item.userId(),
                        item.username(),
                        item.roomId(),
                        item.seatId(),
                        item.timeSlotId(),
                        item.timeSlotLabel(),
                        item.date().toString(),
                        item.status(),
                        item.createdAt() != null ? item.createdAt().toString() : null,
                        item.checkInAt() != null ? item.checkInAt().toString() : null,
                        item.checkOutAt() != null ? item.checkOutAt().toString() : null
                ))
                .toList();

        responseContext.set(Response.ok(new AdminReservationListResponse(dtos)).build());
    }
}
