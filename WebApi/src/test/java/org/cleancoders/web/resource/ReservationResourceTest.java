package org.cleancoders.web.resource;

import jakarta.ws.rs.core.Response;
import org.cleancoders.reservation.usecase.ReserveUseCase;
import org.cleancoders.web.dto.reservation.ReserveInput;
import org.cleancoders.web.presenter.ResponseContext;
import org.cleancoders.web.presenter.WebApiReservationPresenter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReservationResourceTest {

    private ReservationResource resource;
    private WebApiReservationPresenter presenter;
    private ResponseContext ctx;
    private boolean executeCalled;
    private ReserveUseCase.Request lastRequest;
    private ReserveUseCase.Output outputToReturn;

    @BeforeEach
    void setUp() {
        ctx = new ResponseContext();
        presenter = new WebApiReservationPresenter();
        presenter.responseContext = ctx;
        executeCalled = false;
        lastRequest = null;

        resource = new ReservationResource();
        resource.responseContext = ctx;
        resource.reserveUseCase = new ReserveUseCase() {
            @Override
            public Output execute(Request request) {
                executeCalled = true;
                lastRequest = request;
                return outputToReturn;
            }
        };
    }

    @Test
    void reserveShouldDelegateToUseCaseWithCorrectRequest() {
        outputToReturn = new ReserveUseCase.Output("res-1");
        presenter.success("res-1", "A-1", "上午 08:00-12:00");

        Response response = resource.reserve("jwt.token.here",
                new ReserveInput("seat-1", "ts-1", "2026-07-02"));

        assertTrue(executeCalled);
        assertEquals("jwt.token.here", lastRequest.token());
        assertEquals("seat-1", lastRequest.seatId());
        assertEquals("ts-1", lastRequest.timeSlotId());
        assertEquals(LocalDate.of(2026, 7, 2), lastRequest.date());
        assertEquals(201, response.getStatus());
    }

    @Test
    void reserveShouldReturn201OnSuccess() {
        outputToReturn = new ReserveUseCase.Output("res-2");
        presenter.success("res-2", "B-1", "下午 13:00-17:00");

        Response response = resource.reserve("jwt.token.here",
                new ReserveInput("seat-9", "ts-2", "2026-07-03"));

        assertEquals(201, response.getStatus());
    }

    @Test
    void reserveShouldReturn409OnConflict() {
        outputToReturn = null;
        presenter.seatNotAvailable("seat-1", "上午 08:00-12:00");

        Response response = resource.reserve("jwt.token.here",
                new ReserveInput("seat-1", "ts-1", "2026-07-02"));

        assertEquals(409, response.getStatus());
    }

    @Test
    void reserveShouldReturn400OnInvalidDateFormat() {
        Response response = resource.reserve("jwt.token.here",
                new ReserveInput("seat-1", "ts-1", "not-a-date"));

        assertEquals(400, response.getStatus());
    }

    @Test
    void reserveShouldReturn409OnDuplicateReservation() {
        outputToReturn = null;
        presenter.duplicateReservation("existing-res-1");

        Response response = resource.reserve("jwt.token.here",
                new ReserveInput("seat-1", "ts-1", "2026-07-02"));

        assertEquals(409, response.getStatus());
    }
}
