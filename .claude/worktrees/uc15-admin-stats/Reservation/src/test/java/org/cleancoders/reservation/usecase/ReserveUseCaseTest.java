package org.cleancoders.reservation.usecase;

import org.cleancoders.common.domain.User;
import org.cleancoders.common.domain.UserRole;
import org.cleancoders.common.usecase.AuthUseCase;
import org.cleancoders.common.usecase.StudentAuthUseCase;
import org.cleancoders.common_reservation_seatAndRoom.domain.Seat;
import org.cleancoders.common_reservation_seatAndRoom.domain.SeatStatus;
import org.cleancoders.common_reservation_seatAndRoom.domain.TimeSlot;
import org.cleancoders.userandauth_test_infrastructure.StubTokenService;
import org.cleancoders.userandauth_test_infrastructure.StubUserRepo;
import org.cleancoders.reservation.domain.Reservation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class ReserveUseCaseTest
{

    private static final String STUDENT_ID = "student-1";
    private static final String STUDENT_TOKEN = "student-1";
    private static final String ADMIN_ID = "admin-1";
    private static final String ADMIN_TOKEN = "jwt:" + ADMIN_ID + ":bob:ADMIN";
    private static final String SEAT_ID = "seat-1";
    private static final String TIME_SLOT_ID = "ts-1";
    private static final LocalDate DATE = LocalDate.of(2026, 7, 2);
    private ReserveUseCase useCase;
    private StubTokenService tokenService;
    private StubUserRepo userRepo;
    private org.cleancoders.seatandroom_test_infrastructure.StubSeatRepo seatRepo;
    private org.cleancoders.seatandroom_test_infrastructure.StubTimeSlotRepo timeSlotRepo;
    private StubReservationRepo reservationRepo;
    private StubPresenter presenter;

    @BeforeEach
    void setUp()
    {
        tokenService = new StubTokenService();
        tokenService.setUserId(STUDENT_ID);
        userRepo = new org.cleancoders.userandauth_test_infrastructure.StubUserRepo();
        seatRepo = new org.cleancoders.seatandroom_test_infrastructure.StubSeatRepo();
        timeSlotRepo = new org.cleancoders.seatandroom_test_infrastructure.StubTimeSlotRepo();
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

        // Default setup: valid student, available seat, valid time slot
        userRepo.addUser(new User(STUDENT_ID, "alice", "hashed", UserRole.STUDENT, "Alice", "a@b.com"));
        userRepo.addUser(new User(ADMIN_ID, "bob", "hashed", UserRole.ADMIN, "Bob", "b@b.com"));
        seatRepo.addSeat(new Seat(SEAT_ID, "room-1", "A-1", SeatStatus.AVAILABLE));
        timeSlotRepo.addTimeSlot(new TimeSlot(TIME_SLOT_ID, "08:00", "12:00", "上午 08:00-12:00"));
    }

    @Test
    void shouldCreateReservationSuccessfully()
    {
        var output = useCase.execute(new ReserveUseCase.Request(
                STUDENT_TOKEN, SEAT_ID, TIME_SLOT_ID, DATE));

        assertNotNull(output);
        assertNotNull(output.reservationId());
        assertEquals("A-1", presenter.successSeatNumber.get());
        assertEquals("上午 08:00-12:00", presenter.successTimeSlot.get());
    }

    @Test
    void shouldRejectSeatNotFound()
    {
        var output = useCase.execute(new ReserveUseCase.Request(
                STUDENT_TOKEN, "nonexistent-seat", TIME_SLOT_ID, DATE));

        assertNull(output);
        assertTrue(presenter.seatNotFoundCalled);
        assertEquals("nonexistent-seat", presenter.seatNotFoundSeatId.get());
    }

    @Test
    void shouldRejectTimeSlotNotFound()
    {
        var output = useCase.execute(new ReserveUseCase.Request(
                STUDENT_TOKEN, SEAT_ID, "nonexistent-ts", DATE));

        assertNull(output);
        assertTrue(presenter.timeSlotNotFoundCalled);
        assertEquals("nonexistent-ts", presenter.timeSlotNotFoundTimeSlotId.get());
    }

    @Test
    void shouldRejectMaintenanceSeat()
    {
        seatRepo.addSeat(new Seat("seat-maint", "room-1", "A-M", SeatStatus.MAINTENANCE));

        var output = useCase.execute(new ReserveUseCase.Request(
                STUDENT_TOKEN, "seat-maint", TIME_SLOT_ID, DATE));

        assertNull(output);
        assertTrue(presenter.seatNotAvailableCalled);
        assertEquals("seat-maint", presenter.seatNotAvailableSeatId.get());
    }

    @Test
    void shouldRejectDuplicateReservationBySameUser()
    {
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
    void shouldRejectSeatAlreadyReservedByOtherUser()
    {
        // Pre-create an active reservation for the same seat+date+timeslot by another user
        Reservation existing = new Reservation("r-other", "other-user", SEAT_ID,
                TIME_SLOT_ID, DATE);
        reservationRepo.addReservation(existing);

        var output = useCase.execute(new ReserveUseCase.Request(
                STUDENT_TOKEN, SEAT_ID, TIME_SLOT_ID, DATE));

        assertNull(output);
        assertTrue(presenter.seatNotAvailableCalled);
    }

    // --- Stubs ---

    static class StubPresenter implements
            ReserveUseCase.Presenter,
            StudentAuthUseCase.Presenter,
            AuthUseCase.Presenter
    {
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

        @Override
        public void success(String reservationId, String seatNumber, String timeSlot)
        {
            successReservationId.set(reservationId);
            successSeatNumber.set(seatNumber);
            successTimeSlot.set(timeSlot);
        }

        @Override
        public void seatNotAvailable(String seatId, String timeSlot)
        {
            seatNotAvailableCalled = true;
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
        public void seatNotFound(String seatId)
        {
            seatNotFoundCalled = true;
            seatNotFoundSeatId.set(seatId);
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
