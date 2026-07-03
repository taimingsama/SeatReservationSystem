package org.cleancoders.reservation.usecase;

import org.cleancoders.reservation.domain.Reservation;
import org.cleancoders.reservation.domain.ReservationStatus;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class CheckInUseCaseTest
{

    private static final String STUDENT_ID = "student-1";
    private static final String STUDENT_TOKEN = "student-1";
    private static final String OTHER_STUDENT_ID = "student-2";
    private static final String SEAT_ID = "seat-1";
    private static final String TIME_SLOT_ID = "ts-1";
    private static final LocalDate DATE = LocalDate.of(2026, 7, 2);
    // Time slot: 08:00-12:00
    private static final LocalTime SLOT_START = LocalTime.of(8, 0);
    private static final LocalTime SLOT_END = LocalTime.of(12, 0);
    private TestableCheckInUseCase useCase;
    private StubTokenService tokenService;
    private org.cleancoders.userandauth_test_infrastructure.StubUserRepo userRepo;
    private StubReservationRepo reservationRepo;
    private StubTimeSlotRepo timeSlotRepo;
    private StubSeatRepo seatRepo;
    private StubPresenter presenter;

    @BeforeEach
    void setUp()
    {
        tokenService = new StubTokenService();
        tokenService.setUserId(STUDENT_ID);
        userRepo = new org.cleancoders.userandauth_test_infrastructure.StubUserRepo();
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
        ((StudentAuthUseCase<?, ?>) useCase).presenter = presenter;
        ((AuthUseCase<?, ?>) useCase).presenter = presenter;

        // Default: slot is 08:00-12:00, current time is 10:00 (within slot)
        useCase.setCurrentTime(LocalDateTime.of(DATE, LocalTime.of(10, 0)));

        // Seed data
        userRepo.addUser(new User(STUDENT_ID, "alice", "hashed", UserRole.STUDENT, "Alice", "a@b.com"));
        userRepo.addUser(new User(OTHER_STUDENT_ID, "charlie", "hashed", UserRole.STUDENT, "Charlie", "c@c.com"));
        seatRepo.addSeat(new Seat(SEAT_ID, "room-1", "A-1", SeatStatus.AVAILABLE));
        timeSlotRepo.addTimeSlot(new TimeSlot(TIME_SLOT_ID, "08:00", "12:00", "上午 08:00-12:00"));
    }

    @Test
    void shouldCheckInSuccessfully()
    {
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
    void shouldRejectReservationNotFound()
    {
        var output = useCase.execute(new CheckInUseCase.Request(STUDENT_TOKEN, "nonexistent"));

        assertNull(output);
        assertTrue(presenter.reservationNotFoundCalled);
        assertEquals("nonexistent", presenter.reservationNotFoundReservationId.get());
    }

    @Test
    void shouldRejectNotYourReservation()
    {
        Reservation res = new Reservation("res-1", OTHER_STUDENT_ID, SEAT_ID, TIME_SLOT_ID, DATE);
        reservationRepo.addReservation(res);

        var output = useCase.execute(new CheckInUseCase.Request(STUDENT_TOKEN, "res-1"));

        assertNull(output);
        assertTrue(presenter.notYourReservationCalled);
    }

    @Test
    void shouldRejectAlreadyCheckedIn()
    {
        Reservation res = new Reservation("res-1", STUDENT_ID, SEAT_ID, TIME_SLOT_ID, DATE);
        res.checkIn(); // already checked in
        reservationRepo.addReservation(res);

        var output = useCase.execute(new CheckInUseCase.Request(STUDENT_TOKEN, "res-1"));

        assertNull(output);
        assertTrue(presenter.invalidStatusCalled);
        assertEquals(ReservationStatus.CHECKED_IN, presenter.invalidStatusCurrentStatus.get());
    }

    @Test
    void shouldRejectCancelledReservation()
    {
        Reservation res = new Reservation("res-1", STUDENT_ID, SEAT_ID, TIME_SLOT_ID, DATE);
        res.cancel();
        reservationRepo.addReservation(res);

        var output = useCase.execute(new CheckInUseCase.Request(STUDENT_TOKEN, "res-1"));

        assertNull(output);
        assertTrue(presenter.invalidStatusCalled);
        assertEquals(ReservationStatus.CANCELLED, presenter.invalidStatusCurrentStatus.get());
    }

    @Test
    void shouldRejectCheckInBeforeSlotStarts()
    {
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
    void shouldRejectCheckInAfterSlotEnds()
    {
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
    void shouldAllowCheckInAtSlotStart()
    {
        // Edge case: exactly at slot start time
        useCase.setCurrentTime(LocalDateTime.of(DATE, SLOT_START));

        Reservation res = new Reservation("res-1", STUDENT_ID, SEAT_ID, TIME_SLOT_ID, DATE);
        reservationRepo.addReservation(res);

        var output = useCase.execute(new CheckInUseCase.Request(STUDENT_TOKEN, "res-1"));

        assertNotNull(output);
        assertEquals("res-1", output.reservationId());
    }

    // --- Testable subclass that controls the clock ---

    static class TestableCheckInUseCase extends CheckInUseCase
    {
        private LocalDateTime currentTime = LocalDateTime.now();

        @Override
        protected LocalDateTime getCurrentTime()
        {
            return currentTime;
        }

        void setCurrentTime(LocalDateTime time)
        {
            this.currentTime = time;
        }
    }

    // --- Stubs ---

    static class StubPresenter implements
            CheckInUseCase.Presenter,
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
        boolean checkInNotAvailableCalled = false;
        AtomicReference<String> checkInNotAvailableReason = new AtomicReference<>();

        @Override
        public void success(String reservationId, String seatNumber, String timeSlot)
        {
            successReservationId.set(reservationId);
            successSeatNumber.set(seatNumber);
            successTimeSlot.set(timeSlot);
        }

        @Override
        public void reservationNotFound(String reservationId)
        {
            reservationNotFoundCalled = true;
            reservationNotFoundReservationId.set(reservationId);
        }

        @Override
        public void notYourReservation()
        {
            notYourReservationCalled = true;
        }

        @Override
        public void invalidStatus(ReservationStatus currentStatus)
        {
            invalidStatusCalled = true;
            invalidStatusCurrentStatus.set(currentStatus);
        }

        @Override
        public void checkInNotAvailable(String reason)
        {
            checkInNotAvailableCalled = true;
            checkInNotAvailableReason.set(reason);
        }

        @Override
        public void forbidden()
        {
            fail("forbidden() must not be called — role check is not under the the test");
        }

        @Override
        public void invalidToken()
        {
            fail("invalidToken() must not be called — token validation is not under the test");
        }

        @Override
        public void userNotFound()
        {
            fail("userNotFound() must not be called — token validation is not under the test");
        }
    }
}
