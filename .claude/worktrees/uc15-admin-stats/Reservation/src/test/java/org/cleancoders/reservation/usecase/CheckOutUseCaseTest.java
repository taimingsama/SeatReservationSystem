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
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class CheckOutUseCaseTest {

    private CheckOutUseCase useCase;
    private StubTokenService tokenService;
    private StubUserRepo userRepo;
    private StubReservationRepo reservationRepo;
    private StubSeatRepo seatRepo;
    private StubTimeSlotRepo timeSlotRepo;
    private StubPresenter presenter;

    private static final String STUDENT_ID = "student-1";
    private static final String STUDENT_TOKEN = "jwt:" + STUDENT_ID + ":alice:STUDENT";
    private static final String ADMIN_ID = "admin-1";
    private static final String ADMIN_TOKEN = "jwt:" + ADMIN_ID + ":bob:ADMIN";
    private static final String OTHER_STUDENT_ID = "student-2";
    private static final String OTHER_STUDENT_TOKEN = "jwt:" + OTHER_STUDENT_ID + ":charlie:STUDENT";
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

        useCase = new CheckOutUseCase();
        useCase.tokenService = tokenService;
        useCase.userRepo = userRepo;
        useCase.reservationRepo = reservationRepo;
        useCase.seatRepo = seatRepo;
        useCase.timeSlotRepo = timeSlotRepo;
        useCase.presenter = presenter;
        ((StudentAuthUseCase<?, ?>) useCase).presenter = presenter;
        ((AuthUseCase<?, ?>) useCase).presenter = presenter;

        // Seed data
        userRepo.addUser(new User(STUDENT_ID, "alice", "hashed", UserRole.STUDENT, "Alice", "a@b.com"));
        userRepo.addUser(new User(ADMIN_ID, "bob", "hashed", UserRole.ADMIN, "Bob", "b@b.com"));
        userRepo.addUser(new User(OTHER_STUDENT_ID, "charlie", "hashed", UserRole.STUDENT, "Charlie", "c@c.com"));
        seatRepo.addSeat(new Seat(SEAT_ID, "room-1", "A-1", SeatStatus.OCCUPIED));
        timeSlotRepo.addTimeSlot(new TimeSlot(TIME_SLOT_ID, "08:00", "12:00", "上午 08:00-12:00"));
    }

    @Test
    void shouldCheckOutSuccessfully() {
        Reservation res = new Reservation("res-1", STUDENT_ID, SEAT_ID, TIME_SLOT_ID, DATE);
        res.checkIn(); // transition to CHECKED_IN
        reservationRepo.addReservation(res);

        var output = useCase.execute(new CheckOutUseCase.Request(STUDENT_TOKEN, "res-1"));

        assertNotNull(output);
        assertEquals("res-1", output.reservationId());
        assertEquals("res-1", presenter.successReservationId.get());
        assertEquals("A-1", presenter.successSeatNumber.get());

        // Verify reservation status
        Reservation updated = reservationRepo.findById("res-1").get();
        assertEquals(ReservationStatus.CHECKED_OUT, updated.status());

        // Verify seat is released
        Seat seat = seatRepo.findById(SEAT_ID).get();
        assertEquals(SeatStatus.AVAILABLE, seat.status());
    }

    @Test
    void shouldRejectReservationNotFound() {
        var output = useCase.execute(new CheckOutUseCase.Request(STUDENT_TOKEN, "nonexistent"));

        assertNull(output);
        assertTrue(presenter.reservationNotFoundCalled);
        assertEquals("nonexistent", presenter.reservationNotFoundReservationId.get());
    }

    @Test
    void shouldRejectNotYourReservation() {
        Reservation res = new Reservation("res-1", OTHER_STUDENT_ID, SEAT_ID, TIME_SLOT_ID, DATE);
        res.checkIn();
        reservationRepo.addReservation(res);

        var output = useCase.execute(new CheckOutUseCase.Request(STUDENT_TOKEN, "res-1"));

        assertNull(output);
        assertTrue(presenter.notYourReservationCalled);
    }

    @Test
    void shouldRejectReservationNotCheckedIn() {
        Reservation res = new Reservation("res-1", STUDENT_ID, SEAT_ID, TIME_SLOT_ID, DATE);
        reservationRepo.addReservation(res);

        var output = useCase.execute(new CheckOutUseCase.Request(STUDENT_TOKEN, "res-1"));

        assertNull(output);
        assertTrue(presenter.invalidStatusCalled);
        assertEquals(ReservationStatus.RESERVED, presenter.invalidStatusCurrentStatus.get());
    }

    @Test
    void shouldRejectAlreadyCheckedOut() {
        Reservation res = new Reservation("res-1", STUDENT_ID, SEAT_ID, TIME_SLOT_ID, DATE);
        res.checkIn();
        res.checkOut();
        reservationRepo.addReservation(res);

        var output = useCase.execute(new CheckOutUseCase.Request(STUDENT_TOKEN, "res-1"));

        assertNull(output);
        assertTrue(presenter.invalidStatusCalled);
        assertEquals(ReservationStatus.CHECKED_OUT, presenter.invalidStatusCurrentStatus.get());
    }

    @Test
    void shouldRejectCancelledReservation() {
        Reservation res = new Reservation("res-1", STUDENT_ID, SEAT_ID, TIME_SLOT_ID, DATE);
        res.cancel();
        reservationRepo.addReservation(res);

        var output = useCase.execute(new CheckOutUseCase.Request(STUDENT_TOKEN, "res-1"));

        assertNull(output);
        assertTrue(presenter.invalidStatusCalled);
        assertEquals(ReservationStatus.CANCELLED, presenter.invalidStatusCurrentStatus.get());
    }

    @Test
    void shouldRejectInvalidToken() {
        var output = useCase.execute(new CheckOutUseCase.Request("bad-token", "res-1"));

        assertNull(output);
        assertTrue(presenter.invalidTokenCalled);
    }

    @Test
    void shouldRejectAdminUser() {
        Reservation res = new Reservation("res-1", ADMIN_ID, SEAT_ID, TIME_SLOT_ID, DATE);
        res.checkIn();
        reservationRepo.addReservation(res);

        var output = useCase.execute(new CheckOutUseCase.Request(ADMIN_TOKEN, "res-1"));

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
            if (token == null || !token.startsWith("jwt:")) {
                throw new TokenValidationException("Invalid token");
            }
            String[] parts = token.split(":");
            if (parts.length != 4) {
                throw new TokenValidationException("Invalid token format");
            }
            return new TokenPayload(parts[1]);
        }
    }

    static class StubUserRepo implements UserRepository {
        private final java.util.Map<String, User> users = new java.util.HashMap<>();

        void addUser(User user) { users.put(user.id(), user); }

        @Override
        public Optional<User> findByUsername(String username) {
            return users.values().stream().filter(u -> u.username().equals(username)).findFirst();
        }

        @Override
        public Optional<User> findById(String id) { return Optional.ofNullable(users.get(id)); }

        @Override
        public User save(User user) { users.put(user.id(), user); return user; }
    }

    static class StubReservationRepo implements ReservationRepository {
        private final java.util.Map<String, Reservation> reservations = new java.util.HashMap<>();

        void addReservation(Reservation r) { reservations.put(r.id(), r); }

        @Override
        public Reservation save(Reservation r) { reservations.put(r.id(), r); return r; }

        @Override
        public Optional<Reservation> findById(String id) { return Optional.ofNullable(reservations.get(id)); }

        @Override
        public Optional<Reservation> findByUserIdAndDateAndTimeSlotIdAndStatusIn(
                String userId, LocalDate date, String timeSlotId, Set<ReservationStatus> statuses) {
            return reservations.values().stream()
                    .filter(r -> r.userId().equals(userId) && r.date().equals(date)
                            && r.timeSlotId().equals(timeSlotId) && statuses.contains(r.status()))
                    .findFirst();
        }

        @Override
        public Optional<Reservation> findBySeatIdAndDateAndTimeSlotIdAndStatusIn(
                String seatId, LocalDate date, String timeSlotId, Set<ReservationStatus> statuses) {
            return reservations.values().stream()
                    .filter(r -> r.seatId().equals(seatId) && r.date().equals(date)
                            && r.timeSlotId().equals(timeSlotId) && statuses.contains(r.status()))
                    .findFirst();
        }

        @Override
        public List<Reservation> findByUserId(String userId) {
            return reservations.values().stream()
                    .filter(r -> r.userId().equals(userId))
                    .toList();
        }

        @Override
        public List<Reservation> findAll() {
            return List.copyOf(reservations.values());
        }
    }

    static class StubSeatRepo implements SeatRepository {
        private final java.util.Map<String, Seat> seats = new java.util.HashMap<>();

        void addSeat(Seat seat) { seats.put(seat.id(), seat); }

        @Override
        public Optional<Seat> findById(String id) { return Optional.ofNullable(seats.get(id)); }

        @Override
        public Seat save(Seat seat) { seats.put(seat.id(), seat); return seat; }

        @Override
        public List<Seat> findByRoomId(String roomId) {
            return seats.values().stream().filter(s -> s.roomId().equals(roomId)).toList();
        }
    }

    static class StubTimeSlotRepo implements TimeSlotRepository {
        private final java.util.Map<String, TimeSlot> slots = new java.util.HashMap<>();

        void addTimeSlot(TimeSlot slot) { slots.put(slot.id(), slot); }

        @Override
        public Optional<TimeSlot> findById(String id) { return Optional.ofNullable(slots.get(id)); }

        @Override
        public List<TimeSlot> findAll() { return List.copyOf(slots.values()); }
    }

    static class StubPresenter implements
            CheckOutUseCase.Presenter,
            StudentAuthUseCase.Presenter,
            AuthUseCase.Presenter
    {
        AtomicReference<String> successReservationId = new AtomicReference<>();
        AtomicReference<String> successSeatNumber = new AtomicReference<>();
        AtomicReference<String> successTimeSlot = new AtomicReference<>();
        boolean reservationNotFoundCalled = false;
        AtomicReference<String> reservationNotFoundReservationId = new AtomicReference<>();
        boolean notYourReservationCalled = false;
        boolean invalidStatusCalled = false;
        AtomicReference<ReservationStatus> invalidStatusCurrentStatus = new AtomicReference<>();
        boolean invalidTokenCalled = false;
        boolean userNotFoundCalled = false;
        boolean forbiddenCalled = false;

        @Override
        public void success(String reservationId, String seatNumber, String timeSlot) {
            successReservationId.set(reservationId);
            successSeatNumber.set(seatNumber);
            successTimeSlot.set(timeSlot);
        }

        @Override
        public void reservationNotFound(String reservationId) {
            reservationNotFoundCalled = true;
            reservationNotFoundReservationId.set(reservationId);
        }

        @Override
        public void notYourReservation() { notYourReservationCalled = true; }

        @Override
        public void invalidStatus(ReservationStatus currentStatus) {
            invalidStatusCalled = true;
            invalidStatusCurrentStatus.set(currentStatus);
        }

        @Override
        public void forbidden() { forbiddenCalled = true; }

        @Override
        public void invalidToken() { invalidTokenCalled = true; }

        @Override
        public void userNotFound() { userNotFoundCalled = true; }
    }
}
