package org.cleancoders.web.presenter;

import jakarta.ws.rs.core.Response;
import org.cleancoders.reservation.domain.ReservationStatus;
import org.cleancoders.web.dto.DuplicateReservationResponse;
import org.cleancoders.web.dto.ErrorResponse;
import org.cleancoders.web.dto.InvalidStatusResponse;
import org.cleancoders.web.dto.ReservationCreatedResponse;
import org.cleancoders.web.dto.ReservationNotFoundResponse;
import org.cleancoders.web.dto.SeatConflictResponse;
import org.cleancoders.web.dto.SeatNotFoundResponse;
import org.cleancoders.web.dto.TimeSlotNotFoundResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WebApiReservationPresenterTest {

    private WebApiReservationPresenter presenter;

    @BeforeEach
    void setUp() {
        presenter = new WebApiReservationPresenter();
    }

    @Test
    void successShouldReturn201WithReservationJson() {
        presenter.success("res-123", "A-1", "上午 08:00-12:00");

        Response response = presenter.getResponse();
        assertEquals(201, response.getStatus());

        ReservationCreatedResponse entity = (ReservationCreatedResponse) response.getEntity();
        assertEquals("res-123", entity.reservationId());
        assertEquals("A-1", entity.seatNumber());
        assertEquals("上午 08:00-12:00", entity.timeSlot());
    }

    @Test
    void seatNotAvailableShouldReturn409() {
        presenter.seatNotAvailable("seat-1", "上午 08:00-12:00");

        Response response = presenter.getResponse();
        assertEquals(409, response.getStatus());

        SeatConflictResponse entity = (SeatConflictResponse) response.getEntity();
        assertEquals("座位已被预约", entity.error());
        assertEquals("seat-1", entity.seatId());
        assertEquals("上午 08:00-12:00", entity.timeSlot());
    }

    @Test
    void duplicateReservationShouldReturn409() {
        presenter.duplicateReservation("existing-res-456");

        Response response = presenter.getResponse();
        assertEquals(409, response.getStatus());

        DuplicateReservationResponse entity = (DuplicateReservationResponse) response.getEntity();
        assertEquals("该时段已有预约", entity.error());
        assertEquals("existing-res-456", entity.existingReservationId());
    }

    @Test
    void timeSlotNotFoundShouldReturn404() {
        presenter.timeSlotNotFound("ts-nonexistent");

        Response response = presenter.getResponse();
        assertEquals(404, response.getStatus());

        TimeSlotNotFoundResponse entity = (TimeSlotNotFoundResponse) response.getEntity();
        assertEquals("时段不存在", entity.error());
        assertEquals("ts-nonexistent", entity.timeSlotId());
    }

    @Test
    void seatNotFoundShouldReturn404() {
        presenter.seatNotFound("seat-nonexistent");

        Response response = presenter.getResponse();
        assertEquals(404, response.getStatus());

        SeatNotFoundResponse entity = (SeatNotFoundResponse) response.getEntity();
        assertEquals("座位不存在", entity.error());
        assertEquals("seat-nonexistent", entity.seatId());
    }

    @Test
    void forbiddenShouldReturn403() {
        presenter.forbidden();

        Response response = presenter.getResponse();
        assertEquals(403, response.getStatus());

        ErrorResponse entity = (ErrorResponse) response.getEntity();
        assertEquals("权限不足，仅学生可创建预约", entity.error());
    }

    @Test
    void invalidTokenShouldReturn401() {
        presenter.invalidToken();

        Response response = presenter.getResponse();
        assertEquals(401, response.getStatus());

        ErrorResponse entity = (ErrorResponse) response.getEntity();
        assertEquals("Invalid or expired token", entity.error());
    }

    @Test
    void userNotFoundShouldReturn404() {
        presenter.userNotFound();

        Response response = presenter.getResponse();
        assertEquals(404, response.getStatus());

        ErrorResponse entity = (ErrorResponse) response.getEntity();
        assertEquals("User not found", entity.error());
    }

    // --- CheckInUseCase.Presenter ---

    @Test
    void reservationNotFoundShouldReturn404() {
        presenter.reservationNotFound("res-unknown");

        Response response = presenter.getResponse();
        assertEquals(404, response.getStatus());

        ReservationNotFoundResponse entity = (ReservationNotFoundResponse) response.getEntity();
        assertEquals("预约不存在", entity.error());
        assertEquals("res-unknown", entity.reservationId());
    }

    @Test
    void notYourReservationShouldReturn403() {
        presenter.notYourReservation();

        Response response = presenter.getResponse();
        assertEquals(403, response.getStatus());

        ErrorResponse entity = (ErrorResponse) response.getEntity();
        assertEquals("只能签到自己的预约", entity.error());
    }

    @Test
    void invalidStatusShouldReturn409() {
        presenter.invalidStatus(ReservationStatus.CHECKED_IN);

        Response response = presenter.getResponse();
        assertEquals(409, response.getStatus());

        InvalidStatusResponse entity = (InvalidStatusResponse) response.getEntity();
        assertEquals("当前状态不允许签到", entity.error());
        assertEquals(ReservationStatus.CHECKED_IN, entity.currentStatus());
    }

    @Test
    void checkInNotAvailableShouldReturn409() {
        presenter.checkInNotAvailable("已过时段结束时间，无法签到");

        Response response = presenter.getResponse();
        assertEquals(409, response.getStatus());

        ErrorResponse entity = (ErrorResponse) response.getEntity();
        assertEquals("已过时段结束时间，无法签到", entity.error());
    }
}
