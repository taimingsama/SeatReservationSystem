package org.cleancoders.web.presenter;

import jakarta.ws.rs.core.Response;
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

        @SuppressWarnings("unchecked")
        var entity = (java.util.Map<String, Object>) response.getEntity();
        assertEquals("res-123", entity.get("reservationId"));
        assertEquals("A-1", entity.get("seatNumber"));
        assertEquals("上午 08:00-12:00", entity.get("timeSlot"));
    }

    @Test
    void seatNotAvailableShouldReturn409() {
        presenter.seatNotAvailable("seat-1", "上午 08:00-12:00");

        Response response = presenter.getResponse();
        assertEquals(409, response.getStatus());

        @SuppressWarnings("unchecked")
        var entity = (java.util.Map<String, Object>) response.getEntity();
        assertEquals("座位已被预约", entity.get("error"));
        assertEquals("seat-1", entity.get("seatId"));
        assertEquals("上午 08:00-12:00", entity.get("timeSlot"));
    }

    @Test
    void duplicateReservationShouldReturn409() {
        presenter.duplicateReservation("existing-res-456");

        Response response = presenter.getResponse();
        assertEquals(409, response.getStatus());

        @SuppressWarnings("unchecked")
        var entity = (java.util.Map<String, Object>) response.getEntity();
        assertEquals("该时段已有预约", entity.get("error"));
        assertEquals("existing-res-456", entity.get("existingReservationId"));
    }

    @Test
    void timeSlotNotFoundShouldReturn404() {
        presenter.timeSlotNotFound("ts-nonexistent");

        Response response = presenter.getResponse();
        assertEquals(404, response.getStatus());

        @SuppressWarnings("unchecked")
        var entity = (java.util.Map<String, Object>) response.getEntity();
        assertEquals("时段不存在", entity.get("error"));
        assertEquals("ts-nonexistent", entity.get("timeSlotId"));
    }

    @Test
    void seatNotFoundShouldReturn404() {
        presenter.seatNotFound("seat-nonexistent");

        Response response = presenter.getResponse();
        assertEquals(404, response.getStatus());

        @SuppressWarnings("unchecked")
        var entity = (java.util.Map<String, Object>) response.getEntity();
        assertEquals("座位不存在", entity.get("error"));
        assertEquals("seat-nonexistent", entity.get("seatId"));
    }

    @Test
    void forbiddenShouldReturn403() {
        presenter.forbidden();

        Response response = presenter.getResponse();
        assertEquals(403, response.getStatus());

        @SuppressWarnings("unchecked")
        var entity = (java.util.Map<String, Object>) response.getEntity();
        assertEquals("权限不足，仅学生可创建预约", entity.get("error"));
    }

    @Test
    void invalidTokenShouldReturn401() {
        presenter.invalidToken();

        Response response = presenter.getResponse();
        assertEquals(401, response.getStatus());

        @SuppressWarnings("unchecked")
        var entity = (java.util.Map<String, Object>) response.getEntity();
        assertEquals("Invalid or expired token", entity.get("error"));
    }

    @Test
    void userNotFoundShouldReturn404() {
        presenter.userNotFound();

        Response response = presenter.getResponse();
        assertEquals(404, response.getStatus());

        @SuppressWarnings("unchecked")
        var entity = (java.util.Map<String, Object>) response.getEntity();
        assertEquals("User not found", entity.get("error"));
    }
}
