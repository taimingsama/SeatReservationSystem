package org.cleancoders.web.presenter;

import jakarta.ws.rs.core.Response;
import org.cleancoders.reservation.domain.ReservationStatus;
import org.cleancoders.web.dto.common.ErrorResponse;
import org.cleancoders.web.dto.reservation.DuplicateReservationResponse;
import org.cleancoders.web.dto.reservation.InvalidStatusResponse;
import org.cleancoders.web.dto.reservation.ReservationCreatedResponse;
import org.cleancoders.web.dto.reservation.ReservationNotFoundResponse;
import org.cleancoders.web.dto.reservation.SeatConflictResponse;
import org.cleancoders.web.dto.reservation.SeatNotFoundResponse;
import org.cleancoders.web.dto.reservation.TimeSlotNotFoundResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WebApiReservationPresenterTest {

    private WebApiReservationPresenter presenter;
    private ResponseContext responseContext;

    @BeforeEach
    void setUp() {
        responseContext = new ResponseContext();
        presenter = new WebApiReservationPresenter();
        presenter.responseContext = responseContext;
    }

    @Test
    void successShouldReturn201WithReservationJson() {
        presenter.success("res-123", "A-1", "上午 08:00-12:00");

        Response response = responseContext.get();
        assertEquals(201, response.getStatus());

        ReservationCreatedResponse entity = (ReservationCreatedResponse) response.getEntity();
        assertEquals("res-123", entity.reservationId());
        assertEquals("A-1", entity.seatNumber());
        assertEquals("上午 08:00-12:00", entity.timeSlot());
    }

    @Test
    void seatNotAvailableShouldReturn409() {
        presenter.seatNotAvailable("seat-1", "上午 08:00-12:00");

        Response response = responseContext.get();
        assertEquals(409, response.getStatus());

        SeatConflictResponse entity = (SeatConflictResponse) response.getEntity();
        assertEquals("座位已被预约", entity.error());
        assertEquals("seat-1", entity.seatId());
        assertEquals("上午 08:00-12:00", entity.timeSlot());
    }

    @Test
    void duplicateReservationShouldReturn409() {
        presenter.duplicateReservation("existing-res-456");

        Response response = responseContext.get();
        assertEquals(409, response.getStatus());

        DuplicateReservationResponse entity = (DuplicateReservationResponse) response.getEntity();
        assertEquals("该时段已有预约", entity.error());
        assertEquals("existing-res-456", entity.existingReservationId());
    }

    @Test
    void timeSlotNotFoundShouldReturn404() {
        presenter.timeSlotNotFound("ts-nonexistent");

        Response response = responseContext.get();
        assertEquals(404, response.getStatus());

        TimeSlotNotFoundResponse entity = (TimeSlotNotFoundResponse) response.getEntity();
        assertEquals("时段不存在", entity.error());
        assertEquals("ts-nonexistent", entity.timeSlotId());
    }

    @Test
    void seatNotFoundShouldReturn404() {
        presenter.seatNotFound("seat-nonexistent");

        Response response = responseContext.get();
        assertEquals(404, response.getStatus());

        SeatNotFoundResponse entity = (SeatNotFoundResponse) response.getEntity();
        assertEquals("座位不存在", entity.error());
        assertEquals("seat-nonexistent", entity.seatId());
    }

    // --- CheckInUseCase.Presenter ---

    @Test
    void reservationNotFoundShouldReturn404() {
        presenter.reservationNotFound("res-unknown");

        Response response = responseContext.get();
        assertEquals(404, response.getStatus());

        ReservationNotFoundResponse entity = (ReservationNotFoundResponse) response.getEntity();
        assertEquals("预约不存在", entity.error());
        assertEquals("res-unknown", entity.reservationId());
    }

    @Test
    void notYourReservationShouldReturn403() {
        presenter.notYourReservation();

        Response response = responseContext.get();
        assertEquals(403, response.getStatus());

        ErrorResponse entity = (ErrorResponse) response.getEntity();
        assertEquals("只能签到自己的预约", entity.error());
    }

    @Test
    void invalidStatusShouldReturn409() {
        presenter.invalidStatus(ReservationStatus.CHECKED_IN);

        Response response = responseContext.get();
        assertEquals(409, response.getStatus());

        InvalidStatusResponse entity = (InvalidStatusResponse) response.getEntity();
        assertEquals("当前状态不允许签到", entity.error());
        assertEquals(ReservationStatus.CHECKED_IN, entity.currentStatus());
    }

    @Test
    void checkInNotAvailableShouldReturn409() {
        presenter.checkInNotAvailable("已过时段结束时间，无法签到");

        Response response = responseContext.get();
        assertEquals(409, response.getStatus());

        ErrorResponse entity = (ErrorResponse) response.getEntity();
        assertEquals("已过时段结束时间，无法签到", entity.error());
    }
}
