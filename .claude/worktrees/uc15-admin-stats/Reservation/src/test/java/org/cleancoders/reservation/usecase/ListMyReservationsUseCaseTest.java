package org.cleancoders.reservation.usecase;

import org.cleancoders.common.domain.User;
import org.cleancoders.common.domain.UserRole;
import org.cleancoders.common.outbound.TokenPayload;
import org.cleancoders.common.outbound.TokenService;
import org.cleancoders.common.outbound.TokenValidationException;
import org.cleancoders.common.outbound.UserRepository;
import org.cleancoders.common.usecase.AuthUseCase;
import org.cleancoders.common.usecase.StudentAuthUseCase;
import org.cleancoders.common_reservation_seatAndRoom.domain.Seat;
import org.cleancoders.common_reservation_seatAndRoom.domain.SeatStatus;
import org.cleancoders.common_reservation_seatAndRoom.domain.TimeSlot;
import org.cleancoders.common_reservation_seatAndRoom.outbound.SeatRepository;
import org.cleancoders.common_reservation_seatAndRoom.outbound.TimeSlotRepository;
import org.cleancoders.reservation.domain.Reservation;
import org.cleancoders.reservation.domain.ReservationStatus;
import org.cleancoders.reservation.outbound.ReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ListMyReservationsUseCaseTest {

    private ListMyReservationsUseCase useCase;
    private StubTokenService tokenService;
    private StubUserRepo userRepo;
    private StubReservationRepo reservationRepo;
    private StubSeatRepo seatRepo;
    private StubTimeSlotRepo timeSlotRepo;
    private StubPresenter presenter;

    private static final String STUDENT_ID = "student-1";
    private static final String STUDENT_TOKEN = "jwt:" + STUDENT_ID + ":alice:STUDENT";
    private static final String ADMIN_TOKEN = "jwt:admin-1:bob:ADMIN";
    private static final String SEAT_ID = "seat-1";
    private static final String TIME_SLOT_ID = "ts-1";
    private static final LocalDate DATE = LocalDate.of(2026, 7, 2);

    @BeforeEach
    void setUp() {
        tokenService = new StubTokenService();
        userRepo = new StubUserRepo();
        reservationRepo = new StubReservationRepo();
        seatRepo = new StubSeatRepo();
        timeSlotRepo = new StubTimeSlotRepo();
        presenter = new StubPresenter();

        useCase = new ListMyReservationsUseCase();
        useCase.tokenService = tokenService;
        useCase.userRepo = userRepo;
        useCase.reservationRepo = reservationRepo;
        useCase.seatRepo = seatRepo;
        useCase.timeSlotRepo = timeSlotRepo;
        useCase.presenter = presenter;
        ((StudentAuthUseCase<?, ?>) useCase).presenter = presenter;
        ((AuthUseCase<?, ?>) useCase).presenter = presenter;

        userRepo.addUser(new User(STUDENT_ID, "alice", "hashed", UserRole.STUDENT, "Alice", "a@b.com"));
        userRepo.addUser(new User("admin-1", "bob", "hashed", UserRole.ADMIN, "Bob", "b@b.com"));
        seatRepo.addSeat(new Seat(SEAT_ID, "room-1", "A-1", SeatStatus.AVAILABLE));
        timeSlotRepo.addTimeSlot(new TimeSlot(TIME_SLOT_ID, "08:00", "12:00", "上午 08:00-12:00"));
    }

    @Test
    void shouldReturnOwnReservations() {
        reservationRepo.addReservation(new Reservation("res-1", STUDENT_ID, SEAT_ID, TIME_SLOT_ID, DATE));
        reservationRepo.addReservation(new Reservation("res-2", STUDENT_ID, "seat-2", TIME_SLOT_ID, DATE));

        var output = useCase.execute(new ListMyReservationsUseCase.Request(STUDENT_TOKEN));

        assertNotNull(output);
        assertEquals(2, output.items().size());
        assertEquals(2, presenter.items.size());
        assertEquals("res-1", presenter.items.get(0).reservationId());
        assertEquals("A-1", presenter.items.get(0).seatNumber());
    }

    @Test
    void shouldNotReturnOtherUsersReservations() {
        reservationRepo.addReservation(new Reservation("res-1", STUDENT_ID, SEAT_ID, TIME_SLOT_ID, DATE));
        reservationRepo.addReservation(new Reservation("res-2", "other-user", SEAT_ID, TIME_SLOT_ID, DATE));

        var output = useCase.execute(new ListMyReservationsUseCase.Request(STUDENT_TOKEN));

        assertNotNull(output);
        assertEquals(1, output.items().size());
    }

    @Test
    void shouldReturnEmptyListWhenNoReservations() {
        var output = useCase.execute(new ListMyReservationsUseCase.Request(STUDENT_TOKEN));

        assertNotNull(output);
        assertTrue(output.items().isEmpty());
        assertTrue(presenter.items.isEmpty());
    }

    @Test
    void shouldRejectInvalidToken() {
        var output = useCase.execute(new ListMyReservationsUseCase.Request("bad-token"));

        assertNull(output);
        assertTrue(presenter.invalidTokenCalled);
    }

    @Test
    void shouldRejectAdminUser() {
        var output = useCase.execute(new ListMyReservationsUseCase.Request(ADMIN_TOKEN));

        assertNull(output);
        assertTrue(presenter.forbiddenCalled);
    }

    // --- Stubs ---

    static class StubTokenService implements TokenService {
        @Override
        public String generate(String userId) {
            return "jwt:" + userId;
        }

        @Override
        public TokenPayload validate(String token) {
            if (token == null || !token.startsWith("jwt:"))
                throw new TokenValidationException("Invalid");
            String[] p = token.split(":");
            if (p.length != 4)
                throw new TokenValidationException("Invalid");
            return new TokenPayload(p[1]);
        }
    }

    static class StubUserRepo implements UserRepository {
        private final java.util.Map<String, User> m = new java.util.HashMap<>();

        void addUser(User u) { m.put(u.id(), u); }

        @Override
        public Optional<User> findByUsername(String u) {
            return m.values().stream().filter(x -> x.username().equals(u)).findFirst();
        }

        @Override
        public Optional<User> findById(String id) { return Optional.ofNullable(m.get(id)); }

        @Override
        public User save(User u) { m.put(u.id(), u); return u; }
    }

    static class StubReservationRepo implements ReservationRepository {
        private final java.util.Map<String, Reservation> m = new java.util.HashMap<>();

        void addReservation(Reservation r) { m.put(r.id(), r); }

        @Override
        public Reservation save(Reservation r) { m.put(r.id(), r); return r; }

        @Override
        public Optional<Reservation> findById(String id) { return Optional.ofNullable(m.get(id)); }

        @Override
        public Optional<Reservation> findByUserIdAndDateAndTimeSlotIdAndStatusIn(
                String uid, LocalDate d, String ts, Set<ReservationStatus> ss) {
            return m.values().stream().filter(r -> r.userId().equals(uid) && r.date().equals(d)
                    && r.timeSlotId().equals(ts) && ss.contains(r.status())).findFirst();
        }

        @Override
        public Optional<Reservation> findBySeatIdAndDateAndTimeSlotIdAndStatusIn(
                String sid, LocalDate d, String ts, Set<ReservationStatus> ss) {
            return m.values().stream().filter(r -> r.seatId().equals(sid) && r.date().equals(d)
                    && r.timeSlotId().equals(ts) && ss.contains(r.status())).findFirst();
        }

        @Override
        public List<Reservation> findByUserId(String userId) {
            return m.values().stream().filter(r -> r.userId().equals(userId)).toList();
        }

        @Override
        public List<Reservation> findAll() {
            return List.copyOf(m.values());
        }
    }

    static class StubSeatRepo implements SeatRepository {
        private final java.util.Map<String, Seat> m = new java.util.HashMap<>();

        void addSeat(Seat s) { m.put(s.id(), s); }

        @Override
        public Optional<Seat> findById(String id) { return Optional.ofNullable(m.get(id)); }

        @Override
        public Seat save(Seat s) { m.put(s.id(), s); return s; }

        @Override
        public List<Seat> findByRoomId(String rid) {
            return m.values().stream().filter(s -> s.roomId().equals(rid)).toList();
        }
    }

    static class StubTimeSlotRepo implements TimeSlotRepository {
        private final java.util.Map<String, TimeSlot> m = new java.util.HashMap<>();

        void addTimeSlot(TimeSlot ts) { m.put(ts.id(), ts); }

        @Override
        public Optional<TimeSlot> findById(String id) { return Optional.ofNullable(m.get(id)); }

        @Override
        public List<TimeSlot> findAll() { return List.copyOf(m.values()); }
    }

    static class StubPresenter implements
            ListMyReservationsUseCase.Presenter,
            StudentAuthUseCase.Presenter,
            AuthUseCase.Presenter
    {
        List<ListMyReservationsUseCase.ReservationItem> items = List.of();
        boolean invalidTokenCalled, userNotFoundCalled, forbiddenCalled;

        @Override
        public void presentReservations(List<ListMyReservationsUseCase.ReservationItem> items) { this.items = items; }

        @Override
        public void forbidden() { forbiddenCalled = true; }

        @Override
        public void invalidToken() { invalidTokenCalled = true; }

        @Override
        public void userNotFound() { userNotFoundCalled = true; }
    }
}
