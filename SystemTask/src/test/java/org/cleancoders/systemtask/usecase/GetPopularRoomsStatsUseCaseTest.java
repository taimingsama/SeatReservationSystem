package org.cleancoders.systemtask.usecase;

import org.cleancoders.common.domain.User;
import org.cleancoders.common.domain.UserRole;
import org.cleancoders.common.usecase.AdminAuthUseCase;
import org.cleancoders.common.usecase.AuthUseCase;
import org.cleancoders.common_reservation_seatAndRoom.domain.Seat;
import org.cleancoders.common_reservation_seatAndRoom.domain.SeatStatus;
import org.cleancoders.common_reservation_seatandroom_test_infrastructure.StubSeatRepo;
import org.cleancoders.common_test_infrastructure.StubTokenService;
import org.cleancoders.common_test_infrastructure.StubUserRepo;
import org.cleancoders.reservation.domain.Reservation;
import org.cleancoders.seatandroom.domain.RoomStatus;
import org.cleancoders.seatandroom.domain.StudyRoom;
import org.cleancoders.seatandroom.outbound.RoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.*;

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

        roomRepo.addRoom(new StudyRoom("room-1", "自习室A", "一楼", 30, RoomStatus.OPEN));
        roomRepo.addRoom(new StudyRoom("room-2", "自习室B", "二楼", 20, RoomStatus.OPEN));
        seatRepo.addSeat(new Seat("s1", "room-1", "A-1", SeatStatus.AVAILABLE));
        seatRepo.addSeat(new Seat("s2", "room-1", "A-2", SeatStatus.AVAILABLE));
        seatRepo.addSeat(new Seat("s3", "room-2", "B-1", SeatStatus.AVAILABLE));
    }

    @Test
    void shouldRankRoomsByReservationCountDescending()
    {
        LocalDate today = LocalDate.now();
        reservationRepo.save(new Reservation("r1", "u1", "s1", "ts-1", today));
        reservationRepo.save(new Reservation("r2", "u2", "s1", "ts-2", today));
        reservationRepo.save(new Reservation("r3", "u3", "s2", "ts-1", today));
        reservationRepo.save(new Reservation("r4", "u4", "s3", "ts-1", today));

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
        @Override public List<StudyRoom> findAll() { return List.copyOf(store.values()); }
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
    }
}
