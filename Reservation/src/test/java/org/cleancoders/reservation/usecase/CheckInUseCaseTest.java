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
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class CheckInUseCaseTest {

    private TestableCheckInUseCase useCase;
    private StubTokenService tokenService;
    private StubUserRepo userRepo;
    private StubReservationRepo reservationRepo;
    private StubTimeSlotRepo timeSlotRepo;
    private StubSeatRepo seatRepo;
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

    // Time slot: 08:00-12:00
    private static final LocalTime SLOT_START = LocalTime.of(8, 0);
    private static final LocalTime SLOT_END = LocalTime.of(12, 0);

    @BeforeEach
    void setUp() {
        tokenService = new StubTokenService();
        userRepo = new StubUserRepo();
        reservationRepo = new StubReservationRepo();
        timeSlotRepo = new StubTimeSlotRepo();
        seatRepo = new StubSeatRepo();
        presenter = new StubPresenter();

        useCase = new TestableCheckInUseCase();
        useCase.tokenService = tokenService;
        useCase.userRepo = userRepo;
        useCase.reservationRepo = reservationRepo;
        useCase.timeSlotRepo = timeSlotRepo;
        useCase.seatRepo = seatRepo;
        useCase.presenter = presenter;

        // Default: slot is 08:00-12:00, current time is 10:00 (within slot)
        useCase.setCurrentTime(LocalDateTime.of(DATE, LocalTime.of(10, 0)));

        // Seed data
        userRepo.addUser(new User(STUDENT_ID, "alice", "hashed", UserRole.STUDENT, "Alice", "a@b.com"));
        userRepo.addUser(new User(ADMIN_ID, "bob", "hashed", UserRole.ADMIN, "Bob", "b@b.com"));
        userRepo.addUser(new User(OTHER_STUDENT_ID, "charlie", "hashed", UserRole.STUDENT, "Charlie", "c@c.com"));
        seatRepo.addSeat(new Seat(SEAT_ID, "room-1", "A-1", SeatStatus.AVAILABLE));
        timeSlotRepo.addTimeSlot(new TimeSlot(TIME_SLOT_ID, "08:00", "12:00", "上午 08:00-12:00"));
    }

    @Test
    void shouldCheckInSuccessfully() {
        Reservation res = new Reservation("res-1", STUDENT_ID, SEAT_ID, TIME_SLOT_ID, DATE);
        reservationRepo.addReservation(res);

        var output = useCase.execute(new CheckInUseCase.Request(STUDENT_TOKEN, "res-1"));

        assertNotNull(output);
        assertEquals("res-1", output.reservationId());
        assertEquals("res-1", presenter.successReservationId.get());
        assertEquals("A-1", presenter.successSeatNumber.get());
        assertEquals(ReservationStatus.CHECKED_IN, reservationRepo.findById("res-1").get().status());
    }

    @Test
    void shouldRejectReservationNotFound() {
        var output = useCase.execute(new CheckInUseCase.Request(STUDENT_TOKEN, "nonexistent"));

        assertNull(output);
        assertTrue(presenter.reservationNotFoundCalled);
        assertEquals("nonexistent", presenter.reservationNotFoundReservationId.get());
    }

    @Test
    void shouldRejectNotYourReservation() {
        Reservation res = new Reservation("res-1", OTHER_STUDENT_ID, SEAT_ID, TIME_SLOT_ID, DATE);
        reservationRepo.addReservation(res);

        var output = useCase.execute(new CheckInUseCase.Request(STUDENT_TOKEN, "res-1"));

        assertNull(output);
        assertTrue(presenter.notYourReservationCalled);
    }

    @Test
    void shouldRejectAlreadyCheckedIn() {
        Reservation res = new Reservation("res-1", STUDENT_ID, SEAT_ID, TIME_SLOT_ID, DATE);
        res.checkIn(); // already checked in
        reservationRepo.addReservation(res);

        var output = useCase.execute(new CheckInUseCase.Request(STUDENT_TOKEN, "res-1"));

        assertNull(output);
        assertTrue(presenter.invalidStatusCalled);
        assertEquals(ReservationStatus.CHECKED_IN, presenter.invalidStatusCurrentStatus.get());
    }

    @Test
    void shouldRejectCancelledReservation() {
        Reservation res = new Reservation("res-1", STUDENT_ID, SEAT_ID, TIME_SLOT_ID, DATE);
        res.cancel();
        reservationRepo.addReservation(res);

        var output = useCase.execute(new CheckInUseCase.Request(STUDENT_TOKEN, "res-1"));

        assertNull(output);
        assertTrue(presenter.invalidStatusCalled);
        assertEquals(ReservationStatus.CANCELLED, presenter.invalidStatusCurrentStatus.get());
    }

    @Test
    void shouldRejectCheckInBeforeSlotStarts() {
        // Set current time to before the slot (07:00, slot starts at 08:00)
        useCase.setCurrentTime(LocalDateTime.of(DATE, LocalTime.of(7, 0)));

        Reservation res = new Reservation("res-1", STUDENT_ID, SEAT_ID, TIME_SLOT_ID, DATE);
        reservationRepo.addReservation(res);

        var output = useCase.execute(new CheckInUseCase.Request(STUDENT_TOKEN, "res-1"));

        assertNull(output);
        assertTrue(presenter.checkInNotAvailableCalled);
        assertTrue(presenter.checkInNotAvailableReason.get().contains("时段尚未开始"));
    }

    @Test
    void shouldRejectCheckInAfterSlotEnds() {
        // Set current time to after the slot (14:00, slot ends at 12:00)
        useCase.setCurrentTime(LocalDateTime.of(DATE, LocalTime.of(14, 0)));

        Reservation res = new Reservation("res-1", STUDENT_ID, SEAT_ID, TIME_SLOT_ID, DATE);
        reservationRepo.addReservation(res);

        var output = useCase.execute(new CheckInUseCase.Request(STUDENT_TOKEN, "res-1"));

        assertNull(output);
        assertTrue(presenter.checkInNotAvailableCalled);
        assertTrue(presenter.checkInNotAvailableReason.get().contains("已过时段结束时间"));
    }

    @Test
    void shouldAllowCheckInAtSlotStart() {
        // Edge case: exactly at slot start time
        useCase.setCurrentTime(LocalDateTime.of(DATE, SLOT_START));

        Reservation res = new Reservation("res-1", STUDENT_ID, SEAT_ID, TIME_SLOT_ID, DATE);
        reservationRepo.addReservation(res);

        var output = useCase.execute(new CheckInUseCase.Request(STUDENT_TOKEN, "res-1"));

        assertNotNull(output);
        assertEquals("res-1", output.reservationId());
    }

    @Test
    void shouldRejectInvalidToken() {
        var output = useCase.execute(new CheckInUseCase.Request("bad-token", "res-1"));

        assertNull(output);
        assertTrue(presenter.invalidTokenCalled);
    }

    @Test
    void shouldRejectAdminUser() {
        Reservation res = new Reservation("res-1", ADMIN_ID, SEAT_ID, TIME_SLOT_ID, DATE);
        reservationRepo.addReservation(res);

        var output = useCase.execute(new CheckInUseCase.Request(ADMIN_TOKEN, "res-1"));

        assertNull(output);
        assertTrue(presenter.forbiddenCalled);
    }

    // --- Testable subclass that controls the clock ---

    static class TestableCheckInUseCase extends CheckInUseCase {
        private LocalDateTime currentTime = LocalDateTime.now();

        void setCurrentTime(LocalDateTime time) {
            this.currentTime = time;
        }

        @Override
        protected LocalDateTime getCurrentTime() {
            return currentTime;
        }
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

    static class StubReservationRepo implements ReservationRepository {
        private final java.util.Map<String, Reservation> reservations = new java.util.HashMap<>();

        void addReservation(Reservation r) {
            reservations.put(r.id(), r);
        }

        @Override
        public Reservation save(Reservation reservation) {
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

        @Override
        public List<Reservation> findByUserId(String userId) {
            return reservations.values().stream()
                    .filter(r -> r.userId().equals(userId))
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

    static class StubPresenter implements CheckInUseCase.Presenter {
        AtomicReference<String> successReservationId = new AtomicReference<>();
        AtomicReference<String> successSeatNumber = new AtomicReference<>();
        AtomicReference<String> successTimeSlot = new AtomicReference<>();
        boolean reservationNotFoundCalled = false;
        AtomicReference<String> reservationNotFoundReservationId = new AtomicReference<>();
        boolean notYourReservationCalled = false;
        boolean invalidStatusCalled = false;
        AtomicReference<ReservationStatus> invalidStatusCurrentStatus = new AtomicReference<>();
        boolean checkInNotAvailableCalled = false;
        AtomicReference<String> checkInNotAvailableReason = new AtomicReference<>();
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
        public void notYourReservation() {
            notYourReservationCalled = true;
        }

        @Override
        public void invalidStatus(ReservationStatus currentStatus) {
            invalidStatusCalled = true;
            invalidStatusCurrentStatus.set(currentStatus);
        }

        @Override
        public void checkInNotAvailable(String reason) {
            checkInNotAvailableCalled = true;
            checkInNotAvailableReason.set(reason);
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
