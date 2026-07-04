package org.cleancoders.reservation.usecase;

import org.cleancoders.reservation.domain.Reservation;
import org.cleancoders.reservation.domain.ReservationStatus;
import org.cleancoders.reservation.outbound.ReservationRepository;
import org.cleancoders.seatandroom.domain.Seat;
import org.cleancoders.seatandroom.domain.SeatStatus;
import org.cleancoders.seatandroom.domain.TimeSlot;
import org.cleancoders.seatandroom_test_infrastructure.StubSeatRepo;
import org.cleancoders.seatandroom_test_infrastructure.StubTimeSlotRepo;
import org.cleancoders.userandauth.domain.User;
import org.cleancoders.userandauth.domain.UserRole;
import org.cleancoders.userandauth.usecase.AuthUseCase;
import org.cleancoders.userandauth.usecase.StudentAuthUseCase;
import org.cleancoders.userandauth_test_infrastructure.StubTokenService;
import org.cleancoders.userandauth_test_infrastructure.StubUserRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class ReserveUseCaseTest
{

    private static final String STUDENT_ID = "student-1";
    private static final String STUDENT_TOKEN = "jwt:" + STUDENT_ID + ":alice:STUDENT";
    private static final String ADMIN_ID = "admin-1";
    private static final String ADMIN_TOKEN = "jwt:" + ADMIN_ID + ":bob:ADMIN";
    private static final String ROOM_ID = "room-1";
    private static final int SEAT_ID = 1;
    private static final String TIME_SLOT_ID = "ts-1";
    private static final LocalDate DATE = LocalDate.of(2026, 7, 2);
    private ReserveUseCase useCase;
    private StubTokenService tokenService;
    private StubUserRepo userRepo;
    private StubSeatRepo seatRepo;
    private StubTimeSlotRepo timeSlotRepo;
    private StubReservationRepo reservationRepo;
    private StubPresenter presenter;

    @BeforeEach
    void setUp()
    {
        tokenService = new StubTokenService();
        tokenService.setUserId(STUDENT_ID);
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
        ((StudentAuthUseCase<?, ?>) useCase).presenter = presenter;
        ((AuthUseCase<?, ?>) useCase).presenter = presenter;

        userRepo.addUser(new User(STUDENT_ID, "alice", "hashed", UserRole.STUDENT, "Alice", "a@b.com"));
        userRepo.addUser(new User(ADMIN_ID, "bob", "hashed", UserRole.ADMIN, "Bob", "b@b.com"));
        seatRepo.addSeat(new Seat(SEAT_ID, ROOM_ID, SeatStatus.AVAILABLE));
        timeSlotRepo.addTimeSlot(new TimeSlot(TIME_SLOT_ID, "08:00", "12:00", "上午 08:00-12:00"));
    }

    @Test
    void shouldCreateReservationSuccessfully()
    {
        var output = useCase.execute(new ReserveUseCase.Request(
                STUDENT_TOKEN, ROOM_ID, SEAT_ID, TIME_SLOT_ID, DATE));

        assertNotNull(output);
        assertNotNull(output.reservationId());
        assertEquals("1", presenter.successSeatNumber.get());
        assertEquals("上午 08:00-12:00", presenter.successTimeSlot.get());
    }

    @Test
    void shouldRejectSeatNotFound()
    {
        var output = useCase.execute(new ReserveUseCase.Request(
                STUDENT_TOKEN, "nonexistent-room", 99, TIME_SLOT_ID, DATE));

        assertNull(output);
        assertTrue(presenter.seatNotFoundCalled);
        assertEquals("nonexistent-room", presenter.seatNotFoundRoomId.get());
        assertEquals(99, presenter.seatNotFoundSeatId.get());
    }

    @Test
    void shouldRejectTimeSlotNotFound()
    {
        var output = useCase.execute(new ReserveUseCase.Request(
                STUDENT_TOKEN, ROOM_ID, SEAT_ID, "nonexistent-ts", DATE));

        assertNull(output);
        assertTrue(presenter.timeSlotNotFoundCalled);
        assertEquals("nonexistent-ts", presenter.timeSlotNotFoundTimeSlotId.get());
    }

    @Test
    void shouldRejectMaintenanceSeat()
    {
        seatRepo.addSeat(new Seat(2, ROOM_ID, SeatStatus.MAINTENANCE));

        var output = useCase.execute(new ReserveUseCase.Request(
                STUDENT_TOKEN, ROOM_ID, 2, TIME_SLOT_ID, DATE));

        assertNull(output);
        assertTrue(presenter.seatNotAvailableCalled);
        assertEquals(ROOM_ID, presenter.seatNotAvailableRoomId.get());
        assertEquals(2, presenter.seatNotAvailableSeatId.get());
    }

    @Test
    void shouldRejectDuplicateReservationBySameUser()
    {
        Reservation existing = new Reservation("r-existing", STUDENT_ID, ROOM_ID, 3, TIME_SLOT_ID, DATE);
        reservationRepo.addReservation(existing);

        var output = useCase.execute(new ReserveUseCase.Request(
                STUDENT_TOKEN, ROOM_ID, SEAT_ID, TIME_SLOT_ID, DATE));

        assertNull(output);
        assertTrue(presenter.duplicateReservationCalled);
        assertEquals("r-existing", presenter.duplicateReservationExistingId.get());
    }

    @Test
    void shouldRejectSeatAlreadyReservedByOtherUser()
    {
        Reservation existing = new Reservation("r-other", "other-user", ROOM_ID, SEAT_ID, TIME_SLOT_ID, DATE);
        reservationRepo.addReservation(existing);

        var output = useCase.execute(new ReserveUseCase.Request(
                STUDENT_TOKEN, ROOM_ID, SEAT_ID, TIME_SLOT_ID, DATE));

        assertNull(output);
        assertTrue(presenter.seatNotAvailableCalled);
    }

    // --- Stubs ---

    static class StubReservationRepo implements ReservationRepository
    {
        private final java.util.Map<String, Reservation> m = new java.util.HashMap<>();

        void addReservation(Reservation r) { m.put(r.id(), r); }

        @Override public Reservation save(Reservation r) { if (r.id() == null) r.setId("r-" + m.size()); m.put(r.id(), r); return r; }
        @Override public Optional<Reservation> findById(String id) { return Optional.ofNullable(m.get(id)); }
        @Override
        public Optional<Reservation> findByUserIdAndDateAndTimeSlotIdAndStatusIn(
                String uid, LocalDate d, String ts, Set<ReservationStatus> ss) {
            return m.values().stream()
                    .filter(r -> r.userId().equals(uid) && r.date().equals(d)
                            && r.timeSlotId().equals(ts) && ss.contains(r.status())).findFirst();
        }
        @Override
        public Optional<Reservation> findBySeatIdAndDateAndTimeSlotIdAndStatusIn(
                String roomId, int seatId, LocalDate d, String ts, Set<ReservationStatus> ss) {
            return m.values().stream()
                    .filter(r -> r.roomId().equals(roomId) && r.seatId() == seatId
                            && r.date().equals(d) && r.timeSlotId().equals(ts) && ss.contains(r.status())).findFirst();
        }
        @Override public List<Reservation> findByUserId(String uid) {
            return m.values().stream().filter(r -> r.userId().equals(uid)).toList();
        }
        @Override public List<Reservation> findAll() { return List.copyOf(m.values()); }
        @Override
        public List<Reservation> findBySeatIdAndStatusIn(String roomId, int seatId, Set<ReservationStatus> ss) {
            return m.values().stream()
                    .filter(r -> r.roomId().equals(roomId) && r.seatId() == seatId && ss.contains(r.status())).toList();
        }
    }

    static class StubPresenter implements
            ReserveUseCase.Presenter,
            StudentAuthUseCase.Presenter,
            AuthUseCase.Presenter
    {
        AtomicReference<String> successReservationId = new AtomicReference<>();
        AtomicReference<String> successSeatNumber = new AtomicReference<>();
        AtomicReference<String> successTimeSlot = new AtomicReference<>();
        boolean seatNotAvailableCalled = false;
        AtomicReference<String> seatNotAvailableRoomId = new AtomicReference<>();
        AtomicReference<Integer> seatNotAvailableSeatId = new AtomicReference<>();
        boolean duplicateReservationCalled = false;
        AtomicReference<String> duplicateReservationExistingId = new AtomicReference<>();
        boolean timeSlotNotFoundCalled = false;
        AtomicReference<String> timeSlotNotFoundTimeSlotId = new AtomicReference<>();
        boolean seatNotFoundCalled = false;
        AtomicReference<String> seatNotFoundRoomId = new AtomicReference<>();
        AtomicReference<Integer> seatNotFoundSeatId = new AtomicReference<>();

        @Override
        public void success(String reservationId, String seatNumber, String timeSlot)
        {
            successReservationId.set(reservationId);
            successSeatNumber.set(seatNumber);
            successTimeSlot.set(timeSlot);
        }

        @Override
        public void seatNotAvailable(String roomId, int seatId, String timeSlot)
        {
            seatNotAvailableCalled = true;
            seatNotAvailableRoomId.set(roomId);
            seatNotAvailableSeatId.set(seatId);
        }

        @Override
        public void duplicateReservation(String existingId)
        {
            duplicateReservationCalled = true;
            duplicateReservationExistingId.set(existingId);
        }

        @Override
        public void timeSlotNotFound(String timeSlotId)
        {
            timeSlotNotFoundCalled = true;
            timeSlotNotFoundTimeSlotId.set(timeSlotId);
        }

        @Override
        public void seatNotFound(String roomId, int seatId)
        {
            seatNotFoundCalled = true;
            seatNotFoundRoomId.set(roomId);
            seatNotFoundSeatId.set(seatId);
        }

        boolean creditScoreInsufficientCalled = false;

        @Override
        public void creditScoreInsufficient()
        {
            creditScoreInsufficientCalled = true;
        }

        boolean maxReservationsReachedCalled = false;
        int maxReservationsReachedValue = 0;

        @Override
        public void maxReservationsReached(int max)
        {
            maxReservationsReachedCalled = true;
            maxReservationsReachedValue = max;
        }

        @Override public void forbidden() { fail("forbidden() must not be called"); }
        @Override public void invalidToken() { fail("invalidToken() must not be called"); }
        @Override public void userNotFound() { fail("userNotFound() must not be called"); }
    @Override public void banned() {}
    }
}
