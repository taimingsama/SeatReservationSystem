package org.cleancoders.reservation.usecase;

import org.cleancoders.common.domain.User;
import org.cleancoders.common.domain.UserRole;
import org.cleancoders.reservation.domain.Reservation;
import org.cleancoders.reservation.domain.ReservationStatus;
import org.cleancoders.reservation.outbound.ReservationRepository;
import org.cleancoders.seatandroom.domain.Seat;
import org.cleancoders.seatandroom.domain.SeatStatus;
import org.cleancoders.seatandroom.domain.TimeSlot;
import org.cleancoders.seatandroom.outbound.SeatRepository;
import org.cleancoders.seatandroom.outbound.TimeSlotRepository;
import org.cleancoders.userandauth.outbound.TokenPayload;
import org.cleancoders.userandauth.outbound.TokenService;
import org.cleancoders.userandauth.outbound.TokenValidationException;
import org.cleancoders.userandauth.outbound.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class ReserveUseCaseTest {

    private ReserveUseCase useCase;
    private StubTokenService tokenService;
    private StubUserRepo userRepo;
    private StubSeatRepo seatRepo;
    private StubTimeSlotRepo timeSlotRepo;
    private StubReservationRepo reservationRepo;
    private StubPresenter presenter;

    private static final String STUDENT_ID = "student-1";
    private static final String STUDENT_TOKEN = "jwt:" + STUDENT_ID + ":alice:STUDENT";
    private static final String ADMIN_ID = "admin-1";
    private static final String ADMIN_TOKEN = "jwt:" + ADMIN_ID + ":bob:ADMIN";
    private static final String SEAT_ID = "seat-1";
    private static final String TIME_SLOT_ID = "ts-1";
    private static final LocalDate DATE = LocalDate.of(2026, 7, 2);

    @BeforeEach
    void setUp() {
        tokenService = new StubTokenService();
        userRepo = new StubUserRepo();
        seatRepo = new StubSeatRepo();
        timeSlotRepo = new StubTimeSlotRepo();
        reservationRepo = new StubReservationRepo();
        presenter = new StubPresenter();

        useCase = new ReserveUseCase();
        useCase.tokenService = tokenService;
        useCase.userRepo = userRepo;
        useCase.seatRepo = seatRepo;
        useCase.timeSlotRepo = timeSlotRepo;
        useCase.reservationRepo = reservationRepo;
        useCase.presenter = presenter;

        // Default setup: valid student, available seat, valid time slot
        userRepo.addUser(new User(STUDENT_ID, "alice", "hashed", UserRole.STUDENT, "Alice", "a@b.com"));
        userRepo.addUser(new User(ADMIN_ID, "bob", "hashed", UserRole.ADMIN, "Bob", "b@b.com"));
        seatRepo.addSeat(new Seat(SEAT_ID, "room-1", "A-1", SeatStatus.AVAILABLE));
        timeSlotRepo.addTimeSlot(new TimeSlot(TIME_SLOT_ID, "08:00", "12:00", "上午 08:00-12:00"));
    }

    @Test
    void shouldCreateReservationSuccessfully() {
        var output = useCase.execute(new ReserveUseCase.Request(
                STUDENT_TOKEN, SEAT_ID, TIME_SLOT_ID, DATE));

        assertNotNull(output);
        assertNotNull(output.reservationId());
        assertEquals("A-1", presenter.successSeatNumber.get());
        assertEquals("上午 08:00-12:00", presenter.successTimeSlot.get());
    }

    @Test
    void shouldRejectSeatNotFound() {
        var output = useCase.execute(new ReserveUseCase.Request(
                STUDENT_TOKEN, "nonexistent-seat", TIME_SLOT_ID, DATE));

        assertNull(output);
        assertTrue(presenter.seatNotFoundCalled);
        assertEquals("nonexistent-seat", presenter.seatNotFoundSeatId.get());
    }

    @Test
    void shouldRejectTimeSlotNotFound() {
        var output = useCase.execute(new ReserveUseCase.Request(
                STUDENT_TOKEN, SEAT_ID, "nonexistent-ts", DATE));

        assertNull(output);
        assertTrue(presenter.timeSlotNotFoundCalled);
        assertEquals("nonexistent-ts", presenter.timeSlotNotFoundTimeSlotId.get());
    }

    @Test
    void shouldRejectMaintenanceSeat() {
        seatRepo.addSeat(new Seat("seat-maint", "room-1", "A-M", SeatStatus.MAINTENANCE));

        var output = useCase.execute(new ReserveUseCase.Request(
                STUDENT_TOKEN, "seat-maint", TIME_SLOT_ID, DATE));

        assertNull(output);
        assertTrue(presenter.seatNotAvailableCalled);
        assertEquals("seat-maint", presenter.seatNotAvailableSeatId.get());
    }

    @Test
    void shouldRejectDuplicateReservationBySameUser() {
        // Pre-create an active reservation for the same user+date+timeslot
        Reservation existing = new Reservation("r-existing", STUDENT_ID, "seat-2",
                TIME_SLOT_ID, DATE);
        reservationRepo.addReservation(existing);

        var output = useCase.execute(new ReserveUseCase.Request(
                STUDENT_TOKEN, SEAT_ID, TIME_SLOT_ID, DATE));

        assertNull(output);
        assertTrue(presenter.duplicateReservationCalled);
        assertEquals("r-existing", presenter.duplicateReservationExistingId.get());
    }

    @Test
    void shouldRejectSeatAlreadyReservedByOtherUser() {
        // Pre-create an active reservation for the same seat+date+timeslot by another user
        Reservation existing = new Reservation("r-other", "other-user", SEAT_ID,
                TIME_SLOT_ID, DATE);
        reservationRepo.addReservation(existing);

        var output = useCase.execute(new ReserveUseCase.Request(
                STUDENT_TOKEN, SEAT_ID, TIME_SLOT_ID, DATE));

        assertNull(output);
        assertTrue(presenter.seatNotAvailableCalled);
    }

    @Test
    void shouldRejectInvalidToken() {
        var output = useCase.execute(new ReserveUseCase.Request(
                "bad-token", SEAT_ID, TIME_SLOT_ID, DATE));

        assertNull(output);
        assertTrue(presenter.invalidTokenCalled);
    }

    @Test
    void shouldRejectAdminUser() {
        var output = useCase.execute(new ReserveUseCase.Request(
                ADMIN_TOKEN, SEAT_ID, TIME_SLOT_ID, DATE));

        assertNull(output);
        assertTrue(presenter.forbiddenCalled);
    }

    // --- Stubs ---

    static class StubTokenService implements TokenService {
        @Override
        public String generate(String userId, String username, String role) {
            return "jwt:" + userId + ":" + username + ":" + role;
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
            return new TokenPayload(parts[1], parts[2], parts[3]);
        }
    }

    static class StubUserRepo implements UserRepository {
        private final java.util.Map<String, User> users = new java.util.HashMap<>();

        void addUser(User user) {
            users.put(user.id(), user);
        }

        @Override
        public Optional<User> findByUsername(String username) {
            return users.values().stream()
                    .filter(u -> u.username().equals(username))
                    .findFirst();
        }

        @Override
        public Optional<User> findById(String id) {
            return Optional.ofNullable(users.get(id));
        }

        @Override
        public User save(User user) {
            users.put(user.id(), user);
            return user;
        }
    }

    static class StubSeatRepo implements SeatRepository {
        private final java.util.Map<String, Seat> seats = new java.util.HashMap<>();

        void addSeat(Seat seat) {
            seats.put(seat.id(), seat);
        }

        @Override
        public Optional<Seat> findById(String id) {
            return Optional.ofNullable(seats.get(id));
        }

        @Override
        public Seat save(Seat seat) {
            seats.put(seat.id(), seat);
            return seat;
        }

        @Override
        public List<Seat> findByRoomId(String roomId) {
            return seats.values().stream()
                    .filter(s -> s.roomId().equals(roomId))
                    .toList();
        }
    }

    static class StubTimeSlotRepo implements TimeSlotRepository {
        private final java.util.Map<String, TimeSlot> slots = new java.util.HashMap<>();

        void addTimeSlot(TimeSlot slot) {
            slots.put(slot.id(), slot);
        }

        @Override
        public Optional<TimeSlot> findById(String id) {
            return Optional.ofNullable(slots.get(id));
        }

        @Override
        public List<TimeSlot> findAll() {
            return List.copyOf(slots.values());
        }
    }

    static class StubReservationRepo implements ReservationRepository {
        private final java.util.Map<String, Reservation> reservations = new java.util.HashMap<>();

        void addReservation(Reservation r) {
            reservations.put(r.id(), r);
        }

        @Override
        public Reservation save(Reservation reservation) {
            if (reservation.id() == null) {
                reservation.setId("generated-" + reservations.size());
            }
            reservations.put(reservation.id(), reservation);
            return reservation;
        }

        @Override
        public Optional<Reservation> findById(String id) {
            return Optional.ofNullable(reservations.get(id));
        }

        @Override
        public Optional<Reservation> findByUserIdAndDateAndTimeSlotIdAndStatusIn(
                String userId, LocalDate date, String timeSlotId, Set<ReservationStatus> statuses) {
            return reservations.values().stream()
                    .filter(r -> r.userId().equals(userId))
                    .filter(r -> r.date().equals(date))
                    .filter(r -> r.timeSlotId().equals(timeSlotId))
                    .filter(r -> statuses.contains(r.status()))
                    .findFirst();
        }

        @Override
        public Optional<Reservation> findBySeatIdAndDateAndTimeSlotIdAndStatusIn(
                String seatId, LocalDate date, String timeSlotId, Set<ReservationStatus> statuses) {
            return reservations.values().stream()
                    .filter(r -> r.seatId().equals(seatId))
                    .filter(r -> r.date().equals(date))
                    .filter(r -> r.timeSlotId().equals(timeSlotId))
                    .filter(r -> statuses.contains(r.status()))
                    .findFirst();
        }
    }

    static class StubPresenter implements ReserveUseCase.Presenter {
        AtomicReference<String> successReservationId = new AtomicReference<>();
        AtomicReference<String> successSeatNumber = new AtomicReference<>();
        AtomicReference<String> successTimeSlot = new AtomicReference<>();
        boolean seatNotAvailableCalled = false;
        AtomicReference<String> seatNotAvailableSeatId = new AtomicReference<>();
        boolean duplicateReservationCalled = false;
        AtomicReference<String> duplicateReservationExistingId = new AtomicReference<>();
        boolean timeSlotNotFoundCalled = false;
        AtomicReference<String> timeSlotNotFoundTimeSlotId = new AtomicReference<>();
        boolean seatNotFoundCalled = false;
        AtomicReference<String> seatNotFoundSeatId = new AtomicReference<>();
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
        public void seatNotAvailable(String seatId, String timeSlot) {
            seatNotAvailableCalled = true;
            seatNotAvailableSeatId.set(seatId);
        }

        @Override
        public void duplicateReservation(String existingId) {
            duplicateReservationCalled = true;
            duplicateReservationExistingId.set(existingId);
        }

        @Override
        public void timeSlotNotFound(String timeSlotId) {
            timeSlotNotFoundCalled = true;
            timeSlotNotFoundTimeSlotId.set(timeSlotId);
        }

        @Override
        public void seatNotFound(String seatId) {
            seatNotFoundCalled = true;
            seatNotFoundSeatId.set(seatId);
        }

        @Override
        public void forbidden() {
            forbiddenCalled = true;
        }

        @Override
        public void invalidToken() {
            invalidTokenCalled = true;
        }

        @Override
        public void userNotFound() {
            userNotFoundCalled = true;
        }
    }
}
