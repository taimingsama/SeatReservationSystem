package org.cleancoders.reservation.usecase;

import org.cleancoders.reservation.domain.Reservation;
import org.cleancoders.reservation.domain.ReservationStatus;
import org.cleancoders.reservation.outbound.ReservationRepository;
import org.cleancoders.seatandroom.domain.RoomLayout;
import org.cleancoders.seatandroom.domain.RoomStatus;
import org.cleancoders.seatandroom.domain.Seat;
import org.cleancoders.seatandroom.domain.SeatStatus;
import org.cleancoders.seatandroom.domain.StudyRoom;
import org.cleancoders.seatandroom.domain.TimeSlot;
import org.cleancoders.seatandroom.outbound.RoomRepository;
import org.cleancoders.seatandroom_test_infrastructure.StubRoomRepo;
import org.cleancoders.seatandroom_test_infrastructure.StubSeatRepo;
import org.cleancoders.seatandroom_test_infrastructure.StubTimeSlotRepo;
import org.cleancoders.userandauth.domain.User;
import org.cleancoders.userandauth.domain.UserRole;
import org.cleancoders.userandauth.outbound.UserRepository;
import org.cleancoders.userandauth.usecase.AuthUseCase;
import org.cleancoders.userandauth.usecase.StudentAuthUseCase;
import org.cleancoders.userandauth_test_infrastructure.StubTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ListMyReservationsUseCaseTest
{

    private static final String STUDENT_ID = "student-1";
    private static final String STUDENT_TOKEN = "jwt:" + STUDENT_ID + ":alice:STUDENT";
    private static final String ADMIN_TOKEN = "jwt:admin-1:bob:ADMIN";
    private static final String ROOM_ID = "room-1";
    private static final int SEAT_ID = 1;
    private static final String TIME_SLOT_ID = "ts-1";
    private static final LocalDate DATE = LocalDate.of(2026, 7, 2);
    private ListMyReservationsUseCase useCase;
    private StubTokenService tokenService;
    private StubUserRepo userRepo;
    private StubReservationRepo reservationRepo;
    private StubSeatRepo seatRepo;
    private StubTimeSlotRepo timeSlotRepo;
    private StubRoomRepo roomRepo;
    private StubPresenter presenter;

    @BeforeEach
    void setUp()
    {
        tokenService = new StubTokenService();
        tokenService.setUserId(STUDENT_ID);
        userRepo = new StubUserRepo();
        reservationRepo = new StubReservationRepo();
        seatRepo = new StubSeatRepo();
        timeSlotRepo = new StubTimeSlotRepo();
        roomRepo = new StubRoomRepo();
        presenter = new StubPresenter();

        useCase = new ListMyReservationsUseCase();
        useCase.tokenService = tokenService;
        useCase.userRepo = userRepo;
        useCase.reservationRepo = reservationRepo;
        useCase.timeSlotRepo = timeSlotRepo;
        useCase.roomRepo = roomRepo;
        useCase.presenter = presenter;
        ((StudentAuthUseCase<?, ?>) useCase).presenter = presenter;
        ((AuthUseCase<?, ?>) useCase).presenter = presenter;

        userRepo.addUser(new User(STUDENT_ID, "alice", "hashed", UserRole.STUDENT, "Alice", "a@b.com"));
        userRepo.addUser(new User("admin-1", "bob", "hashed", UserRole.ADMIN, "Bob", "b@b.com"));
        seatRepo.addSeat(new Seat(SEAT_ID, ROOM_ID, SeatStatus.AVAILABLE));
        timeSlotRepo.addTimeSlot(new TimeSlot(TIME_SLOT_ID, "08:00", "12:00", "上午 08:00-12:00"));
        roomRepo.add(new StudyRoom(ROOM_ID, "自习室A", "图书馆一楼", RoomLayout.SMALL, RoomStatus.OPEN));
    }

    @Test
    void shouldReturnOwnReservations()
    {
        reservationRepo.addReservation(new Reservation("res-1", STUDENT_ID, ROOM_ID, SEAT_ID, TIME_SLOT_ID, DATE));
        reservationRepo.addReservation(new Reservation("res-2", STUDENT_ID, ROOM_ID, 2, TIME_SLOT_ID, DATE));

        var output = useCase.execute(new ListMyReservationsUseCase.Request(STUDENT_TOKEN));

        assertNotNull(output);
        assertEquals(2, output.items().size());
        assertEquals(2, presenter.items.size());
        assertEquals("res-1", presenter.items.get(0).reservationId());
        assertEquals(ROOM_ID, presenter.items.get(0).roomId());
        assertEquals("自习室A", presenter.items.get(0).roomName());
        assertEquals("图书馆一楼", presenter.items.get(0).roomLocation());
        assertEquals(SEAT_ID, presenter.items.get(0).seatId());
    }

    @Test
    void shouldNotReturnOtherUsersReservations()
    {
        reservationRepo.addReservation(new Reservation("res-1", STUDENT_ID, ROOM_ID, SEAT_ID, TIME_SLOT_ID, DATE));
        reservationRepo.addReservation(new Reservation("res-2", "other-user", ROOM_ID, SEAT_ID, TIME_SLOT_ID, DATE));

        var output = useCase.execute(new ListMyReservationsUseCase.Request(STUDENT_TOKEN));

        assertNotNull(output);
        assertEquals(1, output.items().size());
    }

    @Test
    void shouldReturnEmptyListWhenNoReservations()
    {
        var output = useCase.execute(new ListMyReservationsUseCase.Request(STUDENT_TOKEN));

        assertNotNull(output);
        assertTrue(output.items().isEmpty());
        assertTrue(presenter.items.isEmpty());
    }

    // --- Stubs ---

    static class StubUserRepo implements UserRepository
    {
        private final java.util.Map<String, User> m = new java.util.HashMap<>();

        void addUser(User u)
        {
            m.put(u.id(), u);
        }

        @Override
        public Optional<User> findByUsername(String u)
        {
            return m.values().stream().filter(x -> x.username().equals(u)).findFirst();
        }

        @Override
        public Optional<User> findById(String id)
        {
            return Optional.ofNullable(m.get(id));
        }

        @Override
        public User save(User u)
        {
            m.put(u.id(), u);
            return u;
        }
    }

    static class StubReservationRepo implements ReservationRepository
    {
        private final java.util.Map<String, Reservation> m = new java.util.HashMap<>();

        void addReservation(Reservation r)
        {
            m.put(r.id(), r);
        }

        @Override
        public Reservation save(Reservation r)
        {
            m.put(r.id(), r);
            return r;
        }

        @Override
        public Optional<Reservation> findById(String id)
        {
            return Optional.ofNullable(m.get(id));
        }

        @Override
        public Optional<Reservation> findByUserIdAndDateAndTimeSlotIdAndStatusIn(
                String uid, LocalDate d, String ts, Set<ReservationStatus> ss)
        {
            return m.values().stream().filter(r -> r.userId().equals(uid) && r.date().equals(d)
                    && r.timeSlotId().equals(ts) && ss.contains(r.status())).findFirst();
        }

        @Override
        public Optional<Reservation> findBySeatIdAndDateAndTimeSlotIdAndStatusIn(
                String roomId, int seatId, LocalDate d, String ts, Set<ReservationStatus> ss)
        {
            return m.values().stream().filter(r -> r.roomId().equals(roomId) && r.seatId() == seatId && r.date().equals(d)
                    && r.timeSlotId().equals(ts) && ss.contains(r.status())).findFirst();
        }

        @Override
        public List<Reservation> findByUserId(String userId)
        {
            return m.values().stream().filter(r -> r.userId().equals(userId)).toList();
        }

        @Override
        public List<Reservation> findBySeatIdAndStatusIn(String roomId, int seatId, Set<ReservationStatus> statuses)
        {
            return m.values().stream()
                    .filter(r -> r.roomId().equals(roomId))
                    .filter(r -> r.seatId() == seatId)
                    .filter(r -> statuses.contains(r.status()))
                    .toList();
        }

        @Override
        public List<Reservation> findAll()
        {
            return List.copyOf(m.values());
        }
    }

    static class StubPresenter implements
            ListMyReservationsUseCase.Presenter,
            StudentAuthUseCase.Presenter,
            AuthUseCase.Presenter
    {
        List<ListMyReservationsUseCase.ReservationItem> items = List.of();
        boolean invalidTokenCalled, userNotFoundCalled, forbiddenCalled;

        @Override
        public void presentReservations(List<ListMyReservationsUseCase.ReservationItem> items)
        {
            this.items = items;
        }

        @Override
        public void forbidden()
        {
            forbiddenCalled = true;
        }

        @Override
        public void invalidToken()
        {
            invalidTokenCalled = true;
        }

        @Override
        public void userNotFound()
        {
            userNotFoundCalled = true;
        }
    @Override public void banned() {}
    }
}
