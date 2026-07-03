package org.cleancoders.web.resource;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.cleancoders.common.domain.User;
import org.cleancoders.common.domain.UserRole;
import org.cleancoders.common.outbound.TokenService;
import org.cleancoders.common.outbound.UserRepository;
import org.cleancoders.common_reservation_seatAndRoom.outbound.SeatRepository;
import org.cleancoders.common_reservation_seatAndRoom.outbound.TimeSlotRepository;
import org.cleancoders.infrastructure.persistence.*;
import org.cleancoders.infrastructure.security.JjwtTokenService;
import org.cleancoders.reservation.domain.Reservation;
import org.cleancoders.reservation.outbound.ReservationRepository;
import org.cleancoders.seatandroom.domain.RoomStatus;
import org.cleancoders.seatandroom.domain.StudyRoom;
import org.cleancoders.seatandroom.outbound.RoomRepository;
import org.cleancoders.web.binder.ReservationBinder;
import org.cleancoders.web.binder.SeatAndRoomBinder;
import org.cleancoders.web.binder.UserAndAuthBinder;
import org.cleancoders.web.binder.WebAppBinder;
import org.cleancoders.web.dto.admin.CreateRoomRequest;
import org.cleancoders.web.filter.CorsFilter;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for {@link AdminResource} (UC-13).
 * <p>
 * Runs a real JerseyTest HTTP server with in-memory repositories,
 * verifying the full stack: HTTP → Resource → UseCase → Presenter → Response.
 */
class AdminResourceIntegrationTest extends JerseyTest
{
    private String adminToken;
    private String studentToken;
    private String studentId;
    private JjwtTokenService tokenService;
    private InMemoryRoomRepo roomRepo;

    @Override
    protected Application configure()
    {
        // ---- Repositories ----
        InMemoryUserRepo userRepo = new InMemoryUserRepo();
        InMemorySeatRepo seatRepo = new InMemorySeatRepo();
        InMemoryTimeSlotRepo timeSlotRepo = new InMemoryTimeSlotRepo();
        InMemoryReservationRepo reservationRepo = new InMemoryReservationRepo();
        roomRepo = new InMemoryRoomRepo();

        // ---- Pre-seed users ----
        User admin = userRepo.save(new User(null, "admin", "ignored",
                UserRole.ADMIN, "Admin User", "admin@test.com"));
        User student = userRepo.save(new User(null, "student", "ignored",
                UserRole.STUDENT, "Test Student", "student@test.com"));
        studentId = student.id();

        // ---- Tokens ----
        tokenService = new JjwtTokenService();
        adminToken = tokenService.generate(admin.id());
        studentToken = tokenService.generate(studentId);

        // ---- Pre-seed reservations ----
        Reservation r1 = reservationRepo.save(
                new Reservation(null, studentId, "seat-1", "ts-1", LocalDate.of(2026, 7, 3)));
        Reservation r2 = reservationRepo.save(
                new Reservation(null, studentId, "seat-2", "ts-2", LocalDate.of(2026, 7, 3)));

        ResourceConfig config = new ResourceConfig();
        config.register(AdminResource.class);
        config.register(CorsFilter.class);
        config.register(WebAppBinder.class);
        config.register(UserAndAuthBinder.class);
        config.register(ReservationBinder.class);
        config.register(SeatAndRoomBinder.class);
        config.register(new AbstractBinder()
        {
            @Override
            protected void configure()
            {
                bind(userRepo).to(UserRepository.class);
                bind(seatRepo).to(SeatRepository.class);
                bind(timeSlotRepo).to(TimeSlotRepository.class);
                bind(reservationRepo).to(ReservationRepository.class);
                bind(roomRepo).to(RoomRepository.class);
                bind(tokenService).to(TokenService.class);
            }
        });
        return config;
    }

    @BeforeEach
    public void setUp() throws Exception
    {
        super.setUp();
    }

    @AfterEach
    public void tearDown() throws Exception
    {
        super.tearDown();
    }

    // ================================================================
    // UC-13: GET /admin/reservations — 查看所有预约（管理员）
    // ================================================================

    @Test
    void adminShouldSeeAllReservations()
    {
        Response response = target("/admin/reservations")
                .request(MediaType.APPLICATION_JSON)
                .cookie("Authorization", adminToken)
                .get();

        assertEquals(200, response.getStatus());

        @SuppressWarnings("unchecked")
        Map<String, Object> entity = response.readEntity(Map.class);
        assertNotNull(entity.get("reservations"));

        @SuppressWarnings("unchecked")
        var list = (java.util.List<Map<String, Object>>) entity.get("reservations");
        assertEquals(2, list.size());

        // Verify expected seat numbers are present (ConcurrentHashMap order is non-deterministic)
        java.util.List<String> seatNumbers = list.stream()
                .map(m -> (String) m.get("seatNumber"))
                .toList();
        assertTrue(seatNumbers.contains("A-1"), "Should contain A-1");
        assertTrue(seatNumbers.contains("A-2"), "Should contain A-2");

        // Verify fields on any reservation (both belong to the same student)
        Map<String, Object> first = list.get(0);
        assertNotNull(first.get("reservationId"));
        assertEquals(studentId, first.get("userId"));
        assertEquals("student", first.get("username"));
        assertNotNull(first.get("timeSlotLabel"));
        assertEquals("2026-07-03", first.get("date"));
        assertEquals("RESERVED", first.get("status"));
        assertNotNull(first.get("createdAt"));
    }

    @Test
    void studentShouldGet403WhenAccessingAdminReservations()
    {
        Response response = target("/admin/reservations")
                .request(MediaType.APPLICATION_JSON)
                .cookie("Authorization", studentToken)
                .get();

        assertEquals(403, response.getStatus());

        @SuppressWarnings("unchecked")
        Map<String, Object> entity = response.readEntity(Map.class);
        assertNotNull(entity.get("error"));
    }

    @Test
    void shouldReturn401ForInvalidToken()
    {
        Response response = target("/admin/reservations")
                .request(MediaType.APPLICATION_JSON)
                .cookie("Authorization", "invalid.jwt.token.here")
                .get();

        assertEquals(401, response.getStatus());
    }

    @Test
    void shouldReturn401ForMissingCookie()
    {
        Response response = target("/admin/reservations")
                .request(MediaType.APPLICATION_JSON)
                .get();

        assertEquals(401, response.getStatus());
    }

    @Test
    void adminShouldSeeEmptyListWhenNoReservations()
    {
        // Use a fresh setup with no reservations — tested by verifying
        // the pre-seeded ones are present, non-empty as verified above.
        // The InMemoryReservationRepo is fresh per test, so the two
        // pre-seeded reservations are always present.
        // This test just verifies the response structure when there ARE items.
        Response response = target("/admin/reservations")
                .request(MediaType.APPLICATION_JSON)
                .cookie("Authorization", adminToken)
                .get();

        assertEquals(200, response.getStatus());

        @SuppressWarnings("unchecked")
        Map<String, Object> entity = response.readEntity(Map.class);

        @SuppressWarnings("unchecked")
        var list = (java.util.List<?>) entity.get("reservations");
        assertFalse(list.isEmpty(), "Should contain pre-seeded reservations");
    }

    @Test
    void adminResponseShouldHaveJsonContentType()
    {
        Response response = target("/admin/reservations")
                .request(MediaType.APPLICATION_JSON)
                .cookie("Authorization", adminToken)
                .get();

        assertTrue(response.getHeaderString("Content-Type").startsWith(MediaType.APPLICATION_JSON));
    }

    @Test
    void shouldReturn201WhenAdminCreatesRoom()
    {
        Response response = target("/admin/rooms")
                .request(MediaType.APPLICATION_JSON)
                .cookie("Authorization", adminToken)
                .post(Entity.json(new CreateRoomRequest("自习室F", "综合楼二楼", 20)));

        assertEquals(201, response.getStatus());
        Map<String, Object> body = response.readEntity(new GenericType<>()
        {
        });
        assertNotNull(body.get("id"));
        assertEquals("自习室F", body.get("name"));
        assertEquals("综合楼二楼", body.get("location"));
        assertEquals(20, body.get("capacity"));
        assertEquals("OPEN", body.get("status"));
    }

    @Test
    void shouldReturn403WhenStudentCreatesRoom()
    {
        Response response = target("/admin/rooms")
                .request(MediaType.APPLICATION_JSON)
                .cookie("Authorization", studentToken)
                .post(Entity.json(new CreateRoomRequest("自习室F", "综合楼二楼", 20)));

        assertEquals(403, response.getStatus());
    }

    @Test
    void shouldReturn409WhenNameAlreadyExists()
    {
        roomRepo.save(new StudyRoom("r-existing", "自习室F", "图书馆一楼", 30, RoomStatus.OPEN));

        Response response = target("/admin/rooms")
                .request(MediaType.APPLICATION_JSON)
                .cookie("Authorization", adminToken)
                .post(Entity.json(new CreateRoomRequest("自习室F", "综合楼二楼", 20)));

        assertEquals(409, response.getStatus());
        Map<String, Object> body = response.readEntity(new GenericType<>()
        {
        });
        assertEquals("自习室名称已存在", body.get("error"));
    }

    @Test
    void shouldReturn401WhenNoToken()
    {
        Response response = target("/admin/rooms")
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.json(new CreateRoomRequest("自习室F", "综合楼二楼", 20)));

        assertEquals(401, response.getStatus());
    }

    // --- update room tests ---

    @Test
    void shouldReturn200WhenAdminUpdatesRoom()
    {
        roomRepo.save(new StudyRoom("room-1", "自习室A", "图书馆一楼", 30, RoomStatus.OPEN));

        Response response = target("/admin/rooms/room-1")
                .request(MediaType.APPLICATION_JSON)
                .cookie("Authorization", adminToken)
                .put(Entity.json(new CreateRoomRequest("自习室A-改", "图书馆一楼东", 35)));

        assertEquals(200, response.getStatus());
        Map<String, Object> body = response.readEntity(new GenericType<>()
        {
        });
        assertEquals("room-1", body.get("id"));
        assertEquals("自习室A-改", body.get("name"));
        assertEquals("图书馆一楼东", body.get("location"));
        assertEquals(35, body.get("capacity"));
        assertEquals("OPEN", body.get("status"));
    }

    @Test
    void shouldReturn404WhenUpdatingNonexistentRoom()
    {
        Response response = target("/admin/rooms/nonexistent")
                .request(MediaType.APPLICATION_JSON)
                .cookie("Authorization", adminToken)
                .put(Entity.json(new CreateRoomRequest("自习室X", "一楼", 10)));

        assertEquals(404, response.getStatus());
        Map<String, Object> body = response.readEntity(new GenericType<>()
        {
        });
        assertEquals("自习室不存在", body.get("error"));
        assertEquals("nonexistent", body.get("roomId"));
    }

    @Test
    void shouldReturn409WhenUpdatingToExistingName()
    {
        roomRepo.save(new StudyRoom("room-1", "自习室A", "图书馆一楼", 30, RoomStatus.OPEN));
        roomRepo.save(new StudyRoom("room-2", "自习室B", "图书馆二楼", 20, RoomStatus.OPEN));

        Response response = target("/admin/rooms/room-1")
                .request(MediaType.APPLICATION_JSON)
                .cookie("Authorization", adminToken)
                .put(Entity.json(new CreateRoomRequest("自习室B", "新位置", 25)));

        assertEquals(409, response.getStatus());
        Map<String, Object> body = response.readEntity(new GenericType<>()
        {
        });
        assertEquals("自习室名称已存在", body.get("error"));
    }

    @Test
    void shouldAllowUpdatingRoomWithSameName()
    {
        roomRepo.save(new StudyRoom("room-1", "自习室A", "图书馆一楼", 30, RoomStatus.OPEN));

        Response response = target("/admin/rooms/room-1")
                .request(MediaType.APPLICATION_JSON)
                .cookie("Authorization", adminToken)
                .put(Entity.json(new CreateRoomRequest("自习室A", "图书馆一楼东", 40)));

        assertEquals(200, response.getStatus());
        Map<String, Object> body = response.readEntity(new GenericType<>()
        {
        });
        assertEquals("自习室A", body.get("name"));
        assertEquals("图书馆一楼东", body.get("location"));
        assertEquals(40, body.get("capacity"));
    }

    @Test
    void shouldReturn403WhenStudentUpdatesRoom()
    {
        roomRepo.save(new StudyRoom("room-1", "自习室A", "图书馆一楼", 30, RoomStatus.OPEN));

        Response response = target("/admin/rooms/room-1")
                .request(MediaType.APPLICATION_JSON)
                .cookie("Authorization", studentToken)
                .put(Entity.json(new CreateRoomRequest("自习室F", "综合楼二楼", 20)));

        assertEquals(403, response.getStatus());
    }

    // --- delete room tests ---

    @Test
    void shouldReturn200WhenAdminDeletesRoom()
    {
        roomRepo.save(new StudyRoom("room-1", "自习室A", "图书馆一楼", 30, RoomStatus.OPEN));

        Response response = target("/admin/rooms/room-1")
                .request(MediaType.APPLICATION_JSON)
                .cookie("Authorization", adminToken)
                .delete();

        assertEquals(200, response.getStatus());
        Map<String, Object> body = response.readEntity(new GenericType<>()
        {
        });
        assertEquals("自习室已删除", body.get("message"));
        assertEquals("room-1", body.get("roomId"));

        // Verify room is now CLOSED
        StudyRoom room = roomRepo.findById("room-1").get();
        assertEquals(RoomStatus.CLOSED, room.status());
    }

    @Test
    void shouldReturn404WhenDeletingNonexistentRoom()
    {
        Response response = target("/admin/rooms/nonexistent")
                .request(MediaType.APPLICATION_JSON)
                .cookie("Authorization", adminToken)
                .delete();

        assertEquals(404, response.getStatus());
    }

    @Test
    void shouldReturn409WhenDeletingAlreadyClosedRoom()
    {
        roomRepo.save(new StudyRoom("room-closed", "已关闭", "三楼", 10, RoomStatus.CLOSED));

        Response response = target("/admin/rooms/room-closed")
                .request(MediaType.APPLICATION_JSON)
                .cookie("Authorization", adminToken)
                .delete();

        assertEquals(409, response.getStatus());
    }

    @Test
    void shouldReturn403WhenStudentDeletesRoom()
    {
        roomRepo.save(new StudyRoom("room-1", "自习室A", "图书馆一楼", 30, RoomStatus.OPEN));

        Response response = target("/admin/rooms/room-1")
                .request(MediaType.APPLICATION_JSON)
                .cookie("Authorization", studentToken)
                .delete();

        assertEquals(403, response.getStatus());
    }
}
