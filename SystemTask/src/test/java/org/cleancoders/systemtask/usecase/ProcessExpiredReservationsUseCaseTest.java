package org.cleancoders.systemtask.usecase;

import org.cleancoders.reservation.domain.Reservation;
import org.cleancoders.reservation.domain.ReservationStatus;
import org.cleancoders.reservation.outbound.ReservationRepository;
import org.cleancoders.seatandroom.domain.Seat;
import org.cleancoders.seatandroom.domain.SeatStatus;
import org.cleancoders.seatandroom.domain.TimeSlot;
import org.cleancoders.seatandroom.outbound.SeatRepository;
import org.cleancoders.seatandroom.outbound.TimeSlotRepository;
import org.cleancoders.seatandroom_test_infrastructure.StubSeatRepo;
import org.cleancoders.seatandroom_test_infrastructure.StubTimeSlotRepo;
import org.cleancoders.userandauth.domain.User;
import org.cleancoders.userandauth.domain.UserRole;
import org.cleancoders.userandauth.outbound.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class ProcessExpiredReservationsUseCaseTest {

    private TestableProcessExpiredReservationsUseCase useCase;
    private StubReservationRepo reservationRepo;
    private StubSeatRepo seatRepo;
    private StubTimeSlotRepo timeSlotRepo;
    private StubUserRepo userRepo;

    private static final String USER_ID = "user-1";
    private static final String ROOM_ID = "room-1";
    private static final int SEAT_ID = 1;
    private static final String TS_MORNING = "ts-morning";
    private static final LocalDate TODAY = LocalDate.of(2026, 7, 4);

    @BeforeEach
    void setUp() {
        reservationRepo = new StubReservationRepo();
        seatRepo = new StubSeatRepo();
        timeSlotRepo = new StubTimeSlotRepo();
        userRepo = new StubUserRepo();

        useCase = new TestableProcessExpiredReservationsUseCase();
        useCase.reservationRepo = reservationRepo;
        useCase.seatRepo = seatRepo;
        useCase.timeSlotRepo = timeSlotRepo;
        useCase.userRepo = userRepo;

        // Seed time slot: 08:00-12:00
        timeSlotRepo.addTimeSlot(new TimeSlot(TS_MORNING, "08:00", "12:00", "上午 08:00-12:00"));

        // Seed user
        userRepo.addUser(new User(USER_ID, "alice", "pass", UserRole.STUDENT, "Alice", "a@b.com"));

        // Seed seat (OCCUPIED)
        seatRepo.addSeat(new Seat(SEAT_ID, ROOM_ID, SeatStatus.OCCUPIED));
    }

    @Test
    void autoCheckOutShouldAddStudyHours() {
        // User checked in at 08:30, now is 12:00 (slot ended)
        // Duration = 3.5 hours → toHours() truncates to 3
        Reservation res = new Reservation("res-1", USER_ID, ROOM_ID, SEAT_ID, TS_MORNING, TODAY);
        res.checkIn();
        res.setCheckInAt(LocalDateTime.of(2026, 7, 4, 8, 30));
        reservationRepo.save(res);

        // Set current time to after slot end (12:00)
        useCase.setCurrentTime(LocalDateTime.of(2026, 7, 4, 12, 0));

        var output = useCase.execute();

        assertNotNull(output);
        assertEquals(1, output.autoCheckedOut());
        assertEquals(0, output.expired());

        // Verify studyHours added: 08:30 → 12:00 = 3 hours
        User updated = userRepo.findById(USER_ID).get();
        assertEquals(3, updated.studyHours());

        // Verify credit score: 100 + 5 = 100 (capped)
        assertEquals(100, updated.creditScore());
    }

    @Test
    void autoCheckOutShouldAddStudyHoursBasedOnActualCheckInTime() {
        // User checked in late at 10:00, now is 13:00 (well after slot end)
        // Duration = 3 hours
        Reservation res = new Reservation("res-2", USER_ID, ROOM_ID, SEAT_ID, TS_MORNING, TODAY);
        res.checkIn();
        res.setCheckInAt(LocalDateTime.of(2026, 7, 4, 10, 0));
        reservationRepo.save(res);

        // Slot ended at 12:00, system processes at 13:00
        useCase.setCurrentTime(LocalDateTime.of(2026, 7, 4, 13, 0));

        var output = useCase.execute();

        assertNotNull(output);
        assertEquals(1, output.autoCheckedOut());

        // studyHours = 10:00 → 12:00 (slot end) = 2 hours
        User updated = userRepo.findById(USER_ID).get();
        assertEquals(2, updated.studyHours());
    }

    @Test
    void shouldNotProcessReservationsBeforeSlotEnd() {
        Reservation res = new Reservation("res-3", USER_ID, ROOM_ID, SEAT_ID, TS_MORNING, TODAY);
        res.checkIn();
        res.setCheckInAt(LocalDateTime.of(2026, 7, 4, 8, 0));
        reservationRepo.save(res);

        // Current time is 10:00, slot ends at 12:00 → not yet expired
        useCase.setCurrentTime(LocalDateTime.of(2026, 7, 4, 10, 0));

        var output = useCase.execute();

        assertNotNull(output);
        assertEquals(0, output.autoCheckedOut());
        assertEquals(0, output.expired());

        // studyHours should remain 0
        User updated = userRepo.findById(USER_ID).get();
        assertEquals(0, updated.studyHours());
    }

    @Test
    void expiredReservationShouldNotAddStudyHours() {
        // RESERVED but not checked in → should expire with no study hours
        Reservation res = new Reservation("res-4", USER_ID, ROOM_ID, SEAT_ID, TS_MORNING, TODAY);
        reservationRepo.save(res);

        useCase.setCurrentTime(LocalDateTime.of(2026, 7, 4, 12, 0));

        var output = useCase.execute();

        assertNotNull(output);
        assertEquals(0, output.autoCheckedOut());
        assertEquals(1, output.expired());

        // studyHours should remain 0 (no check-in = no study time)
        User updated = userRepo.findById(USER_ID).get();
        assertEquals(0, updated.studyHours());

        // Credit score should be deducted: 100 - 15 = 85
        assertEquals(85, updated.creditScore());

        // Reservation should be EXPIRED
        Reservation updatedRes = reservationRepo.findById("res-4").get();
        assertEquals(ReservationStatus.EXPIRED, updatedRes.status());
    }

    @Test
    void autoCheckOutShouldReleaseSeat() {
        Reservation res = new Reservation("res-5", USER_ID, ROOM_ID, SEAT_ID, TS_MORNING, TODAY);
        res.checkIn();
        res.setCheckInAt(LocalDateTime.of(2026, 7, 4, 8, 0));
        reservationRepo.save(res);

        useCase.setCurrentTime(LocalDateTime.of(2026, 7, 4, 12, 0));

        useCase.execute();

        Seat seat = seatRepo.findByRoomIdAndSeatId(ROOM_ID, SEAT_ID).get();
        assertEquals(SeatStatus.AVAILABLE, seat.status());
    }

    @Test
    void shouldSkipReservationsOnOtherDates() {
        // Reservation from yesterday — should not be processed
        Reservation res = new Reservation("res-6", USER_ID, ROOM_ID, SEAT_ID, TS_MORNING, TODAY.minusDays(1));
        res.checkIn();
        res.setCheckInAt(LocalDateTime.of(2026, 7, 3, 8, 0));
        reservationRepo.save(res);

        useCase.setCurrentTime(LocalDateTime.of(2026, 7, 4, 12, 0));

        var output = useCase.execute();

        assertNotNull(output);
        assertEquals(0, output.autoCheckedOut());
        assertEquals(0, output.expired());
    }

    // --- Testable subclass ---

    static class TestableProcessExpiredReservationsUseCase extends ProcessExpiredReservationsUseCase {
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

    static class StubReservationRepo implements ReservationRepository {
        private final Map<String, Reservation> store = new LinkedHashMap<>();

        @Override
        public Reservation save(Reservation r) {
            if (r.id() == null) throw new IllegalArgumentException("id required");
            store.put(r.id(), r);
            return r;
        }

        @Override
        public Optional<Reservation> findById(String id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public Optional<Reservation> findByUserIdAndDateAndTimeSlotIdAndStatusIn(
                String uid, LocalDate d, String ts, Set<ReservationStatus> ss) {
            return Optional.empty();
        }

        @Override
        public Optional<Reservation> findBySeatIdAndDateAndTimeSlotIdAndStatusIn(
                String roomId, int seatId, LocalDate d, String ts, Set<ReservationStatus> ss) {
            return Optional.empty();
        }

        @Override
        public List<Reservation> findByUserId(String uid) {
            return List.of();
        }

        @Override
        public List<Reservation> findBySeatIdAndStatusIn(String roomId, int seatId, Set<ReservationStatus> ss) {
            return List.of();
        }

        @Override
        public List<Reservation> findAll() {
            return store.values().stream().toList();
        }
    }

    static class StubUserRepo implements UserRepository {
        private final Map<String, User> users = new HashMap<>();

        void addUser(User user) {
            users.put(user.id(), user);
        }

        @Override
        public Optional<User> findByUsername(String username) {
            return users.values().stream().filter(u -> u.username().equals(username)).findFirst();
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

        @Override
        public List<User> findAll() {
            return List.copyOf(users.values());
        }

        @Override
        public void deleteById(String id) {
            users.remove(id);
        }
    }
}
