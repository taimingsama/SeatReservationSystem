package org.cleancoders.web.resource;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.cleancoders.common.domain.User;
import org.cleancoders.common.domain.UserRole;
import org.cleancoders.common.outbound.TokenService;
import org.cleancoders.common.outbound.UserRepository;
import org.cleancoders.common.usecase.AuthUseCase;
import org.cleancoders.common.usecase.StudentAuthUseCase;
import org.cleancoders.common_reservation_seatAndRoom.outbound.SeatRepository;
import org.cleancoders.common_reservation_seatAndRoom.outbound.TimeSlotRepository;
import org.cleancoders.infrastructure.persistence.InMemoryReservationRepo;
import org.cleancoders.infrastructure.persistence.InMemorySeatRepo;
import org.cleancoders.infrastructure.persistence.InMemoryTimeSlotRepo;
import org.cleancoders.infrastructure.persistence.InMemoryUserRepo;
import org.cleancoders.infrastructure.security.JjwtTokenService;
import org.cleancoders.reservation.outbound.ReservationRepository;
import org.cleancoders.reservation.usecase.CancelReservationUseCase;
import org.cleancoders.reservation.usecase.CheckInUseCase;
import org.cleancoders.reservation.usecase.CheckOutUseCase;
import org.cleancoders.reservation.usecase.ListMyReservationsUseCase;
import org.cleancoders.reservation.usecase.ReserveUseCase;
import org.cleancoders.web.filter.CorsFilter;
import org.cleancoders.web.presenter.WebApiReservationPresenter;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for {@link ReservationResource} (UC-08 ~ UC-12).
 * <p>
 * Runs a real JerseyTest HTTP server with in-memory repositories,
 * verifying the full stack: HTTP → Resource → UseCase → Presenter → Response.
 */
class ReservationResourceIntegrationTest extends JerseyTest {

    private String studentToken;
    private String studentId;
    private String otherStudentToken;

    @Override
    protected Application configure() {
        // ---- Repositories ----
        InMemoryUserRepo userRepo = new InMemoryUserRepo();
        InMemorySeatRepo seatRepo = new InMemorySeatRepo();
        InMemoryTimeSlotRepo timeSlotRepo = new InMemoryTimeSlotRepo();
        InMemoryReservationRepo reservationRepo = new InMemoryReservationRepo();

        // ---- Pre-seed users ----
        User student = userRepo.save(new User(null, "student", "ignored",
                UserRole.STUDENT, "Test Student", "student@test.com"));
        User other = userRepo.save(new User(null, "other", "ignored",
                UserRole.STUDENT, "Other Student", "other@test.com"));
        studentId = student.id();

        // ---- Tokens ----
        JjwtTokenService tokenService = new JjwtTokenService();
        studentToken = tokenService.generate(student.id());
        otherStudentToken = tokenService.generate(other.id());

        // ---- Presenter ----
        WebApiReservationPresenter reservationPresenter = new WebApiReservationPresenter();

        ResourceConfig config = new ResourceConfig();
        config.register(ReservationResource.class);
        config.register(CorsFilter.class);
        config.register(new AbstractBinder() {
            @Override
            protected void configure() {
                // Infrastructure → Outbound
                bind(userRepo).to(UserRepository.class);
                bind(seatRepo).to(SeatRepository.class);
                bind(timeSlotRepo).to(TimeSlotRepository.class);
                bind(reservationRepo).to(ReservationRepository.class);
                bind(tokenService).to(TokenService.class);

                // Use cases (class-based binding — HK2 creates + injects them)
                bind(ReserveUseCase.class).to(ReserveUseCase.class);
                bind(CheckInUseCase.class).to(CheckInUseCase.class);
                bind(CheckOutUseCase.class).to(CheckOutUseCase.class);
                bind(CancelReservationUseCase.class).to(CancelReservationUseCase.class);
                bind(ListMyReservationsUseCase.class).to(ListMyReservationsUseCase.class);

                // Presenter (single instance shared across all interfaces)
                bind(reservationPresenter).to(WebApiReservationPresenter.class);
                bind(reservationPresenter).to(ReserveUseCase.Presenter.class);
                bind(reservationPresenter).to(CheckInUseCase.Presenter.class);
                bind(reservationPresenter).to(CheckOutUseCase.Presenter.class);
                bind(reservationPresenter).to(CancelReservationUseCase.Presenter.class);
                bind(reservationPresenter).to(ListMyReservationsUseCase.Presenter.class);
                bind(reservationPresenter).to(StudentAuthUseCase.Presenter.class);
                bind(reservationPresenter).to(AuthUseCase.Presenter.class);
            }
        });
        return config;
    }

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
    }

    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    // ================================================================
    // UC-08: POST /reservations — 创建预约
    // ================================================================

    @Test
    void reserveShouldReturn201AndCreateReservation() {
        Map<String, String> body = Map.of(
                "seatId", "seat-1",
                "timeSlotId", "ts-1",
                "date", "2026-07-03"
        );

        Response response = target("/reservations")
                .request(MediaType.APPLICATION_JSON)
                .cookie("Authorization", studentToken)
                .post(Entity.json(body));

        assertEquals(201, response.getStatus());

        @SuppressWarnings("unchecked")
        Map<String, Object> entity = response.readEntity(Map.class);
        assertNotNull(entity.get("reservationId"));
        assertEquals("A-1", entity.get("seatNumber"));
        assertEquals("上午 08:00-12:00", entity.get("timeSlot"));
    }

    @Test
    void reserveShouldReturn409WhenSeatAlreadyBooked() {
        Map<String, String> body = Map.of(
                "seatId", "seat-1",
                "timeSlotId", "ts-1",
                "date", "2026-07-03"
        );
        // First reservation succeeds
        target("/reservations")
                .request(MediaType.APPLICATION_JSON)
                .cookie("Authorization", studentToken)
                .post(Entity.json(body));

        // Second reservation for same seat+timeslot+date by different user
        Response response = target("/reservations")
                .request(MediaType.APPLICATION_JSON)
                .cookie("Authorization", otherStudentToken)
                .post(Entity.json(body));

        assertEquals(409, response.getStatus());

        @SuppressWarnings("unchecked")
        Map<String, Object> entity = response.readEntity(Map.class);
        assertEquals("座位已被预约", entity.get("error"));
        assertEquals("seat-1", entity.get("seatId"));
    }

    @Test
    void reserveShouldReturn409OnDuplicateReservationBySameUser() {
        Map<String, String> body = Map.of(
                "seatId", "seat-1",
                "timeSlotId", "ts-1",
                "date", "2026-07-03"
        );
        // First reservation succeeds
        target("/reservations")
                .request(MediaType.APPLICATION_JSON)
                .cookie("Authorization", studentToken)
                .post(Entity.json(body));

        // Same user tries different seat but same timeslot+date
        Map<String, String> body2 = Map.of(
                "seatId", "seat-2",
                "timeSlotId", "ts-1",
                "date", "2026-07-03"
        );
        Response response = target("/reservations")
                .request(MediaType.APPLICATION_JSON)
                .cookie("Authorization", studentToken)
                .post(Entity.json(body2));

        assertEquals(409, response.getStatus());

        @SuppressWarnings("unchecked")
        Map<String, Object> entity = response.readEntity(Map.class);
        assertEquals("该时段已有预约", entity.get("error"));
    }

    @Test
    void reserveShouldReturn400OnInvalidDateFormat() {
        Map<String, String> body = Map.of(
                "seatId", "seat-1",
                "timeSlotId", "ts-1",
                "date", "not-a-date"
        );

        Response response = target("/reservations")
                .request(MediaType.APPLICATION_JSON)
                .cookie("Authorization", studentToken)
                .post(Entity.json(body));

        assertEquals(400, response.getStatus());

        @SuppressWarnings("unchecked")
        Map<String, Object> entity = response.readEntity(Map.class);
        assertTrue(((String) entity.get("error")).contains("日期格式不合法"));
    }

    @Test
    void reserveShouldReturn404WhenSeatNotFound() {
        Map<String, String> body = Map.of(
                "seatId", "seat-nonexistent",
                "timeSlotId", "ts-1",
                "date", "2026-07-03"
        );

        Response response = target("/reservations")
                .request(MediaType.APPLICATION_JSON)
                .cookie("Authorization", studentToken)
                .post(Entity.json(body));

        assertEquals(404, response.getStatus());

        @SuppressWarnings("unchecked")
        Map<String, Object> entity = response.readEntity(Map.class);
        assertEquals("座位不存在", entity.get("error"));
    }

    @Test
    void reserveShouldReturn404WhenTimeSlotNotFound() {
        Map<String, String> body = Map.of(
                "seatId", "seat-1",
                "timeSlotId", "ts-nonexistent",
                "date", "2026-07-03"
        );

        Response response = target("/reservations")
                .request(MediaType.APPLICATION_JSON)
                .cookie("Authorization", studentToken)
                .post(Entity.json(body));

        assertEquals(404, response.getStatus());

        @SuppressWarnings("unchecked")
        Map<String, Object> entity = response.readEntity(Map.class);
        assertEquals("时段不存在", entity.get("error"));
    }

    @Test
    void reserveShouldReturn401ForInvalidToken() {
        Map<String, String> body = Map.of(
                "seatId", "seat-1",
                "timeSlotId", "ts-1",
                "date", "2026-07-03"
        );

        Response response = target("/reservations")
                .request(MediaType.APPLICATION_JSON)
                .cookie("Authorization", "invalid.jwt.token.here")
                .post(Entity.json(body));

        assertEquals(401, response.getStatus());
    }

    @Test
    void reserveShouldReturn401ForMissingCookie() {
        Map<String, String> body = Map.of(
                "seatId", "seat-1",
                "timeSlotId", "ts-1",
                "date", "2026-07-03"
        );

        Response response = target("/reservations")
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.json(body));

        assertEquals(401, response.getStatus());
    }

    // ================================================================
    // UC-09: POST /reservations/{id}/check-in — 签到
    // ================================================================

    @Test
    void checkInShouldReturn404WhenReservationNotFound() {
        Response response = target("/reservations/nonexistent-id/check-in")
                .request(MediaType.APPLICATION_JSON)
                .cookie("Authorization", studentToken)
                .post(Entity.json(""));

        assertEquals(404, response.getStatus());
    }

    @Test
    void checkInShouldReturn403WhenNotOwner() {
        String reservationId = createReservation(studentToken, "seat-1", "ts-1", "2026-07-03");

        Response response = target("/reservations/" + reservationId + "/check-in")
                .request(MediaType.APPLICATION_JSON)
                .cookie("Authorization", otherStudentToken)
                .post(Entity.json(""));

        assertEquals(403, response.getStatus());
    }

    @Test
    void checkInShouldReturn401ForInvalidToken() {
        Response response = target("/reservations/any-id/check-in")
                .request(MediaType.APPLICATION_JSON)
                .cookie("Authorization", "bad.token.here")
                .post(Entity.json(""));

        assertEquals(401, response.getStatus());
    }

    // ================================================================
    // UC-10: POST /reservations/{id}/check-out — 退座
    // ================================================================

    @Test
    void checkOutShouldReturn404WhenReservationNotFound() {
        Response response = target("/reservations/nonexistent-id/check-out")
                .request(MediaType.APPLICATION_JSON)
                .cookie("Authorization", studentToken)
                .post(Entity.json(""));

        assertEquals(404, response.getStatus());
    }

    @Test
    void checkOutShouldReturn403WhenNotOwner() {
        String reservationId = createReservation(studentToken, "seat-1", "ts-1", "2026-07-03");

        // Other student tries to check out
        Response response = target("/reservations/" + reservationId + "/check-out")
                .request(MediaType.APPLICATION_JSON)
                .cookie("Authorization", otherStudentToken)
                .post(Entity.json(""));

        assertEquals(403, response.getStatus());
    }

    @Test
    void checkOutShouldReturn409WhenNotCheckedIn() {
        String reservationId = createReservation(studentToken, "seat-1", "ts-1", "2026-07-03");

        // Try to check out without checking in first
        Response response = target("/reservations/" + reservationId + "/check-out")
                .request(MediaType.APPLICATION_JSON)
                .cookie("Authorization", studentToken)
                .post(Entity.json(""));

        assertEquals(409, response.getStatus());
    }

    // ================================================================
    // UC-11: DELETE /reservations/{id} — 取消预约
    // ================================================================

    @Test
    void cancelShouldSucceedForReservedReservation() {
        String reservationId = createReservation(studentToken, "seat-1", "ts-1", "2026-07-03");

        Response response = target("/reservations/" + reservationId)
                .request(MediaType.APPLICATION_JSON)
                .cookie("Authorization", studentToken)
                .delete();

        // success() returns 201 (shared across Reserve/CheckIn/CheckOut/Cancel)
        assertEquals(201, response.getStatus());

        @SuppressWarnings("unchecked")
        Map<String, Object> entity = response.readEntity(Map.class);
        assertNotNull(entity.get("reservationId"));
    }

    @Test
    void cancelShouldReturn404WhenReservationNotFound() {
        Response response = target("/reservations/nonexistent-id")
                .request(MediaType.APPLICATION_JSON)
                .cookie("Authorization", studentToken)
                .delete();

        assertEquals(404, response.getStatus());
    }

    @Test
    void cancelShouldReturn403WhenNotOwner() {
        String reservationId = createReservation(studentToken, "seat-1", "ts-1", "2026-07-03");

        Response response = target("/reservations/" + reservationId)
                .request(MediaType.APPLICATION_JSON)
                .cookie("Authorization", otherStudentToken)
                .delete();

        assertEquals(403, response.getStatus());
    }

    // ================================================================
    // UC-12: GET /reservations/my — 查看我的预约
    // ================================================================

    @Test
    void myReservationsShouldReturnListWithReservations() {
        // Create two reservations
        createReservation(studentToken, "seat-1", "ts-1", "2026-07-03");
        createReservation(studentToken, "seat-2", "ts-2", "2026-07-03");

        Response response = target("/reservations/my")
                .request(MediaType.APPLICATION_JSON)
                .cookie("Authorization", studentToken)
                .get();

        assertEquals(200, response.getStatus());

        @SuppressWarnings("unchecked")
        Map<String, Object> entity = response.readEntity(Map.class);
        assertNotNull(entity.get("reservations"));

        @SuppressWarnings("unchecked")
        var list = (List<Map<String, Object>>) entity.get("reservations");
        assertEquals(2, list.size());

        // Verify expected seat numbers are present (order is non-deterministic in ConcurrentHashMap)
        List<String> seatNumbers = list.stream()
                .map(m -> (String) m.get("seatNumber"))
                .toList();
        assertTrue(seatNumbers.contains("A-1"), "Should contain A-1");
        assertTrue(seatNumbers.contains("A-2"), "Should contain A-2");

        // Verify common fields on one reservation
        Map<String, Object> first = list.get(0);
        assertNotNull(first.get("reservationId"));
        assertNotNull(first.get("timeSlotLabel"));
        assertEquals("2026-07-03", first.get("date"));
        assertEquals("RESERVED", first.get("status"));
        assertNotNull(first.get("createdAt"));
    }

    @Test
    void myReservationsShouldReturnEmptyListWhenNoReservations() {
        Response response = target("/reservations/my")
                .request(MediaType.APPLICATION_JSON)
                .cookie("Authorization", studentToken)
                .get();

        assertEquals(200, response.getStatus());

        @SuppressWarnings("unchecked")
        Map<String, Object> entity = response.readEntity(Map.class);

        @SuppressWarnings("unchecked")
        var list = (List<?>) entity.get("reservations");
        assertTrue(list.isEmpty());
    }

    @Test
    void myReservationsShouldReturn401ForInvalidToken() {
        Response response = target("/reservations/my")
                .request(MediaType.APPLICATION_JSON)
                .cookie("Authorization", "invalid.token.here")
                .get();

        assertEquals(401, response.getStatus());
    }

    @Test
    void myReservationsShouldOnlyReturnOwnReservations() {
        // Student creates a reservation
        createReservation(studentToken, "seat-1", "ts-1", "2026-07-03");
        // Other student creates a reservation
        createReservation(otherStudentToken, "seat-2", "ts-2", "2026-07-03");

        // Student sees only their own reservation
        Response response = target("/reservations/my")
                .request(MediaType.APPLICATION_JSON)
                .cookie("Authorization", studentToken)
                .get();

        assertEquals(200, response.getStatus());

        @SuppressWarnings("unchecked")
        Map<String, Object> entity = response.readEntity(Map.class);

        @SuppressWarnings("unchecked")
        var list = (List<Map<String, Object>>) entity.get("reservations");
        assertEquals(1, list.size());
        assertEquals("A-1", list.get(0).get("seatNumber"));
    }

    // ================================================================
    // Helpers
    // ================================================================

    /**
     * Creates a reservation via the API and returns the reservation ID.
     */
    private String createReservation(String token, String seatId, String timeSlotId, String date) {
        Map<String, String> body = Map.of(
                "seatId", seatId,
                "timeSlotId", timeSlotId,
                "date", date
        );
        Response response = target("/reservations")
                .request(MediaType.APPLICATION_JSON)
                .cookie("Authorization", token)
                .post(Entity.json(body));

        assertEquals(201, response.getStatus(), "Failed to create reservation");

        @SuppressWarnings("unchecked")
        Map<String, Object> entity = response.readEntity(Map.class);
        return (String) entity.get("reservationId");
    }
}
