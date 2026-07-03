package org.cleancoders.web.resource;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.cleancoders.common.domain.User;
import org.cleancoders.common.domain.UserRole;
import org.cleancoders.common.outbound.UserRepository;
import org.cleancoders.common_reservation_seatAndRoom.outbound.SeatRepository;
import org.cleancoders.common_reservation_seatAndRoom.outbound.TimeSlotRepository;
import org.cleancoders.infrastructure.persistence.InMemoryRoomRepo;
import org.cleancoders.infrastructure.persistence.InMemorySeatRepo;
import org.cleancoders.infrastructure.persistence.InMemoryTimeSlotRepo;
import org.cleancoders.infrastructure.persistence.InMemoryUserRepo;
import org.cleancoders.infrastructure.security.JjwtTokenService;
import org.cleancoders.seatandroom.domain.RoomStatus;
import org.cleancoders.seatandroom.domain.StudyRoom;
import org.cleancoders.seatandroom.outbound.RoomRepository;
import org.cleancoders.web.binder.ReservationBinder;
import org.cleancoders.web.binder.SeatAndRoomBinder;
import org.cleancoders.web.binder.UserAndAuthBinder;
import org.cleancoders.web.binder.WebAppBinder;
import org.cleancoders.web.dto.admin.CreateRoomRequest;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AdminResourceIntegrationTest extends JerseyTest
{

    private InMemoryRoomRepo roomRepo;
    private JjwtTokenService tokenService;

    @Override
    protected Application configure()
    {
        roomRepo = new InMemoryRoomRepo();
        var seatRepo = new InMemorySeatRepo();
        var timeSlotRepo = new InMemoryTimeSlotRepo();
        var userRepo = new InMemoryUserRepo();
        tokenService = new JjwtTokenService();

        userRepo.save(new User("admin-1", "admin", "admin123", UserRole.ADMIN, "Admin", "admin@example.com"));
        userRepo.save(new User("student-1", "alice", "pass123", UserRole.STUDENT, "Alice", "a@b.com"));

        var config = new ResourceConfig();
        config.register(RoomResource.class);
        config.register(AdminResource.class);
        config.register(WebAppBinder.class);
        config.register(SeatAndRoomBinder.class);
        config.register(UserAndAuthBinder.class);
        config.register(ReservationBinder.class);

        config.register(new AbstractBinder()
        {
            @Override
            protected void configure()
            {
                bind(roomRepo).to(RoomRepository.class);
                bind(seatRepo).to(SeatRepository.class);
                bind(timeSlotRepo).to(TimeSlotRepository.class);
                bind(userRepo).to(UserRepository.class);
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

    @Test
    void shouldReturn201WhenAdminCreatesRoom()
    {
        String adminToken = tokenService.generate("admin-1");

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
        String studentToken = tokenService.generate("student-1");

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

        String adminToken = tokenService.generate("admin-1");

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
        String adminToken = tokenService.generate("admin-1");

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
        String adminToken = tokenService.generate("admin-1");

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
        String adminToken = tokenService.generate("admin-1");

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
        String adminToken = tokenService.generate("admin-1");

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
        String studentToken = tokenService.generate("student-1");

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
        String adminToken = tokenService.generate("admin-1");

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
        String adminToken = tokenService.generate("admin-1");

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
        String adminToken = tokenService.generate("admin-1");

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
        String studentToken = tokenService.generate("student-1");

        Response response = target("/admin/rooms/room-1")
                .request(MediaType.APPLICATION_JSON)
                .cookie("Authorization", studentToken)
                .delete();

        assertEquals(403, response.getStatus());
    }
}
