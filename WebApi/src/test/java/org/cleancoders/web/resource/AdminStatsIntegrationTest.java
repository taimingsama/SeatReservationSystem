package org.cleancoders.web.resource;

import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.cleancoders.infrastructure.persistence.*;
import org.cleancoders.infrastructure.security.JjwtTokenService;
import org.cleancoders.reservation.outbound.ReservationRepository;
import org.cleancoders.seatandroom.outbound.RoomRepository;
import org.cleancoders.seatandroom.outbound.SeatRepository;
import org.cleancoders.seatandroom.outbound.TimeSlotRepository;
import org.cleancoders.userandauth.domain.User;
import org.cleancoders.userandauth.domain.UserRole;
import org.cleancoders.userandauth.outbound.UserRepository;
import org.cleancoders.web.binder.*;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AdminStatsIntegrationTest extends JerseyTest
{

    private InMemoryRoomRepo roomRepo;
    private InMemorySeatRepo seatRepo;
    private InMemoryReservationRepo reservationRepo;
    private JjwtTokenService tokenService;

    @Override
    protected Application configure()
    {
        roomRepo = new InMemoryRoomRepo();
        seatRepo = new InMemorySeatRepo();
        var timeSlotRepo = new InMemoryTimeSlotRepo();
        reservationRepo = new InMemoryReservationRepo();
        var userRepo = new InMemoryUserRepo();
        tokenService = new JjwtTokenService();

        userRepo.save(new User("admin-1", "admin", "admin123", UserRole.ADMIN, "Admin", "admin@test.com"));
        userRepo.save(new User("student-1", "alice", "pass123", UserRole.STUDENT, "Alice", "a@b.com"));

        var config = new ResourceConfig();
        config.register(RoomResource.class);
        config.register(AdminResource.class);
        config.register(AdminStatsResource.class);
        config.register(WebAppBinder.class);
        config.register(SeatAndRoomBinder.class);
        config.register(UserAndAuthBinder.class);
        config.register(ReservationBinder.class);
        config.register(SystemTaskBinder.class);

        config.register(new AbstractBinder()
        {
            @Override
            protected void configure()
            {
                bind(roomRepo).to(RoomRepository.class);
                bind(seatRepo).to(SeatRepository.class);
                bind(timeSlotRepo).to(TimeSlotRepository.class);
                bind(reservationRepo).to(ReservationRepository.class);
                bind(userRepo).to(UserRepository.class);
            }
        });
        return config;
    }

    @BeforeEach
    public void setUp() throws Exception { super.setUp(); }

    @AfterEach
    public void tearDown() throws Exception { super.tearDown(); }

    @Test
    void shouldReturnSeatUsageForAdmin()
    {
        String token = tokenService.generate("admin-1");
        Response response = target("/admin/stats/seat-usage")
                .request(MediaType.APPLICATION_JSON)
                .cookie("Authorization", token).get();

        assertEquals(200, response.getStatus());
        Map<String, Object> body = response.readEntity(new GenericType<>() {});
        assertEquals(12, body.get("totalSeats"));
        assertEquals(0, body.get("usedSeats"));
        assertEquals(0.0, body.get("usageRate"));
        assertNotNull(body.get("date"));
    }

    @Test
    void shouldReturnTimeSlotStatsForAdmin()
    {
        String token = tokenService.generate("admin-1");
        Response response = target("/admin/stats/time-slot")
                .request(MediaType.APPLICATION_JSON)
                .cookie("Authorization", token).get();

        assertEquals(200, response.getStatus());
        Map<String, Object> body = response.readEntity(new GenericType<>() {});
        assertNotNull(body.get("date"));
        List<Map<String, Object>> slots = (List<Map<String, Object>>) body.get("timeSlots");
        assertEquals(3, slots.size());
        assertNotNull(slots.get(0).get("timeSlotId"));
        assertNotNull(slots.get(0).get("label"));
        assertNotNull(slots.get(0).get("count"));
    }

    @Test
    void shouldReturnPopularRoomsForAdmin()
    {
        String token = tokenService.generate("admin-1");
        Response response = target("/admin/stats/popular-rooms")
                .request(MediaType.APPLICATION_JSON)
                .cookie("Authorization", token).get();

        assertEquals(200, response.getStatus());
        Map<String, Object> body = response.readEntity(new GenericType<>() {});
        assertNotNull(body.get("date"));
        assertNotNull(body.get("rooms"));
    }

    @Test
    void shouldReturnCheckInRateForAdmin()
    {
        String token = tokenService.generate("admin-1");
        Response response = target("/admin/stats/check-in-rate")
                .request(MediaType.APPLICATION_JSON)
                .cookie("Authorization", token).get();

        assertEquals(200, response.getStatus());
        Map<String, Object> body = response.readEntity(new GenericType<>() {});
        assertNotNull(body.get("date"));
        assertEquals(0, body.get("totalReservations"));
        assertEquals(0.0, body.get("checkInRate"));
    }

    @Test
    void shouldReturnNoShowRateForAdmin()
    {
        String token = tokenService.generate("admin-1");
        Response response = target("/admin/stats/no-show-rate")
                .request(MediaType.APPLICATION_JSON)
                .cookie("Authorization", token).get();

        assertEquals(200, response.getStatus());
        Map<String, Object> body = response.readEntity(new GenericType<>() {});
        assertNotNull(body.get("date"));
        assertEquals(0, body.get("totalReservations"));
        assertEquals(0.0, body.get("noShowRate"));
    }

    @Test
    void shouldReturn401WhenNoToken()
    {
        Response response = target("/admin/stats/seat-usage")
                .request(MediaType.APPLICATION_JSON).get();

        assertEquals(401, response.getStatus());
    }

    @Test
    void shouldReturn403WhenStudentAccesses()
    {
        String token = tokenService.generate("student-1");
        Response response = target("/admin/stats/seat-usage")
                .request(MediaType.APPLICATION_JSON)
                .cookie("Authorization", token).get();

        assertEquals(403, response.getStatus());
    }

    @Test
    void shouldReturn401WhenNoTokenForCheckInRate()
    {
        Response response = target("/admin/stats/check-in-rate")
                .request(MediaType.APPLICATION_JSON).get();

        assertEquals(401, response.getStatus());
    }
}
