package org.cleancoders.systemtask.usecase;

import org.cleancoders.reservation.domain.Reservation;
import org.cleancoders.seatandroom.domain.RoomStatus;
import org.cleancoders.seatandroom.domain.RoomLayout;
import org.cleancoders.seatandroom.domain.Seat;
import org.cleancoders.seatandroom.domain.SeatStatus;
import org.cleancoders.seatandroom.domain.StudyRoom;
import org.cleancoders.seatandroom.outbound.RoomRepository;
import org.cleancoders.seatandroom_test_infrastructure.StubSeatRepo;
import org.cleancoders.userandauth.domain.User;
import org.cleancoders.userandauth.domain.UserRole;
import org.cleancoders.userandauth.usecase.AdminAuthUseCase;
import org.cleancoders.userandauth.usecase.AuthUseCase;
import org.cleancoders.userandauth_test_infrastructure.StubTokenService;
import org.cleancoders.userandauth_test_infrastructure.StubUserRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class GetPopularRoomsStatsUseCaseTest
{

    private GetPopularRoomsStatsUseCase useCase;
    private StubSeatRepo seatRepo;
    private StubRoomRepo roomRepo;
    private GetSeatUsageStatsUseCaseTest.StubReservationRepo reservationRepo;
    private StubPresenter presenter;
    private StubTokenService tokenService;
    private StubUserRepo userRepo;

    @BeforeEach
    void setUp()
    {
        useCase = new GetPopularRoomsStatsUseCase();
        seatRepo = new StubSeatRepo();
        roomRepo = new StubRoomRepo();
        reservationRepo = new GetSeatUsageStatsUseCaseTest.StubReservationRepo();
        presenter = new StubPresenter();
        tokenService = new StubTokenService();
        userRepo = new StubUserRepo();

        useCase.presenter = presenter;
        useCase.seatRepo = seatRepo;
        useCase.roomRepo = roomRepo;
        useCase.reservationRepo = reservationRepo;
        useCase.tokenService = tokenService;
        useCase.userRepo = userRepo;
        ((AdminAuthUseCase<?, ?>) useCase).presenter = presenter;
        ((AuthUseCase<?, ?>) useCase).presenter = presenter;

        User admin = new User("admin-1", "admin", "pass", UserRole.ADMIN, "Admin", "admin@test.com");
        userRepo.addUser(admin);
        tokenService.setUserId(admin.id());

        roomRepo.addRoom(new StudyRoom("room-1", "自习室A", "一楼", RoomLayout.SMALL, RoomStatus.OPEN));
        roomRepo.addRoom(new StudyRoom("room-2", "自习室B", "二楼", RoomLayout.SMALL, RoomStatus.OPEN));
        seatRepo.addSeat(new Seat(1, "room-1", SeatStatus.AVAILABLE));
        seatRepo.addSeat(new Seat(2, "room-1", SeatStatus.AVAILABLE));
        seatRepo.addSeat(new Seat(3, "room-2", SeatStatus.AVAILABLE));
    }

    @Test
    void shouldRankRoomsByReservationCountDescending()
    {
        LocalDate today = LocalDate.now();
        reservationRepo.save(new Reservation("r1", "u1", "room-1", 1, "ts-1", today));
        reservationRepo.save(new Reservation("r2", "u2", "room-1", 2, "ts-2", today));
        reservationRepo.save(new Reservation("r3", "u3", "room-1", 1, "ts-1", today));
        reservationRepo.save(new Reservation("r4", "u4", "room-2", 3, "ts-1", today));

        GetPopularRoomsStatsUseCase.Output output = useCase.execute(
                new GetPopularRoomsStatsUseCase.Request("valid-token"));

        assertNotNull(output);
        assertEquals(2, output.items().size());
        assertEquals("room-1", output.items().get(0).roomId());
        assertEquals(3, output.items().get(0).reservationCount());
        assertEquals("room-2", output.items().get(1).roomId());
        assertEquals(1, output.items().get(1).reservationCount());
    }

    @Test
    void shouldReturnEmptyWhenNoTodayReservations()
    {
        GetPopularRoomsStatsUseCase.Output output = useCase.execute(
                new GetPopularRoomsStatsUseCase.Request("valid-token"));

        assertNotNull(output);
        assertTrue(output.items().isEmpty());
    }

    @Test
    void shouldReturn403WhenNotAdmin()
    {
        User student = new User("student-1", "alice", "pass", UserRole.STUDENT, "Alice", "a@b.com");
        userRepo.addUser(student);
        tokenService.setUserId(student.id());

        GetPopularRoomsStatsUseCase.Output output = useCase.execute(
                new GetPopularRoomsStatsUseCase.Request("valid-token"));

        assertNull(output);
        assertTrue(presenter.forbiddenCalled);
    }

    // --- Stubs ---

    static class StubRoomRepo implements RoomRepository
    {
        private final Map<String, StudyRoom> store = new LinkedHashMap<>();
        void addRoom(StudyRoom room) { store.put(room.id(), room); }
        @Override public List<StudyRoom> findByStatus(RoomStatus s) { return List.of(); }
        @Override public Optional<StudyRoom> findById(String id) { return Optional.ofNullable(store.get(id)); }
        @Override public StudyRoom save(StudyRoom room) { store.put(room.id(), room); return room; }
        @Override public Optional<StudyRoom> findByName(String n) { return Optional.empty(); }
        @Override public List<StudyRoom> findAll() { return store.values().stream().toList(); }
    }

    static class StubPresenter implements GetPopularRoomsStatsUseCase.Presenter,
            AdminAuthUseCase.Presenter, AuthUseCase.Presenter
    {
        boolean presentPopularRoomsCalled;
        LocalDate date;
        List<GetPopularRoomsStatsUseCase.PopularRoomItem> items;
        boolean forbiddenCalled, invalidTokenCalled, userNotFoundCalled;

        @Override
        public void presentPopularRooms(LocalDate date, List<GetPopularRoomsStatsUseCase.PopularRoomItem> items)
        {
            this.presentPopularRoomsCalled = true;
            this.date = date;
            this.items = items;
        }

        @Override public void forbidden() { forbiddenCalled = true; }
        @Override public void invalidToken() { invalidTokenCalled = true; }
        @Override public void userNotFound() { userNotFoundCalled = true; }
    @Override public void banned() {}
    }
}
