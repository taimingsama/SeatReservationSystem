package org.cleancoders.seatandroom.usecase;

import org.cleancoders.seatandroom.domain.Seat;
import org.cleancoders.seatandroom.domain.SeatStatus;
import org.cleancoders.seatandroom.outbound.ActiveReservationChecker;
import org.cleancoders.seatandroom.test.infrastructure.StubSeatRepo;
import org.cleancoders.userandauth.domain.User;
import org.cleancoders.userandauth.domain.UserRole;
import org.cleancoders.userandauth.usecase.AdminAuthUseCase;
import org.cleancoders.userandauth.usecase.AuthUseCase;
import org.cleancoders.userandauth_test_infrastructure.StubTokenService;
import org.cleancoders.userandauth_test_infrastructure.StubUserRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class DeleteSeatUseCaseTest
{

    private static final String ADMIN_ID = "admin-1";
    private static final String ADMIN_TOKEN = "jwt:" + ADMIN_ID + ":admin:ADMIN";
    private static final String STUDENT_ID = "student-1";
    private static final String STUDENT_TOKEN = "jwt:" + STUDENT_ID + ":alice:STUDENT";

    private DeleteSeatUseCase useCase;
    private StubTokenService tokenService;
    private StubUserRepo userRepo;
    private StubSeatRepo seatRepo;
    private StubActiveReservationChecker activeReservationChecker;
    private StubPresenter presenter;

    @BeforeEach
    void setUp()
    {
        tokenService = new StubTokenService();
        tokenService.setUserId(ADMIN_ID);
        userRepo = new StubUserRepo();
        seatRepo = new StubSeatRepo();
        activeReservationChecker = new StubActiveReservationChecker();
        presenter = new StubPresenter();

        useCase = new DeleteSeatUseCase();
        useCase.tokenService = tokenService;
        useCase.userRepo = userRepo;
        useCase.seatRepo = seatRepo;
        useCase.activeReservationChecker = activeReservationChecker;
        useCase.presenter = presenter;
        ((AdminAuthUseCase<?, ?>) useCase).presenter = presenter;
        ((AuthUseCase<?, ?>) useCase).presenter = presenter;

        userRepo.addUser(new User(ADMIN_ID, "admin", "hashed", UserRole.ADMIN, "Admin", "a@b.com"));
        userRepo.addUser(new User(STUDENT_ID, "alice", "hashed", UserRole.STUDENT, "Alice", "a@b.com"));
    }

    @Test
    void shouldRemoveAvailableSeat()
    {
        seatRepo.addSeat(new Seat("seat-1", "room-1", "A-1", SeatStatus.AVAILABLE));

        var output = useCase.execute(new DeleteSeatUseCase.Request(ADMIN_TOKEN, "seat-1"));

        assertNotNull(output);
        assertEquals("seat-1", output.seatId());
        assertTrue(presenter.deleteSeatSuccessCalled);
        assertEquals("seat-1", presenter.deleteSeatSuccessSeatId.get());
        assertEquals(SeatStatus.REMOVED, seatRepo.findById("seat-1").get().status());
    }

    @Test
    void shouldRemoveMaintenanceSeat()
    {
        seatRepo.addSeat(new Seat("seat-m", "room-1", "M-1", SeatStatus.MAINTENANCE));

        var output = useCase.execute(new DeleteSeatUseCase.Request(ADMIN_TOKEN, "seat-m"));

        assertNotNull(output);
        assertEquals("seat-m", output.seatId());
        assertTrue(presenter.deleteSeatSuccessCalled);
        assertEquals(SeatStatus.REMOVED, seatRepo.findById("seat-m").get().status());
    }

    @Test
    void shouldReturnSeatNotFound()
    {
        var output = useCase.execute(new DeleteSeatUseCase.Request(ADMIN_TOKEN, "nonexistent"));

        assertNull(output);
        assertTrue(presenter.seatNotFoundCalled);
        assertEquals("nonexistent", presenter.seatNotFoundId.get());
    }

    @Test
    void shouldReturnSeatAlreadyRemoved()
    {
        seatRepo.addSeat(new Seat("seat-r", "room-1", "R-1", SeatStatus.REMOVED));

        var output = useCase.execute(new DeleteSeatUseCase.Request(ADMIN_TOKEN, "seat-r"));

        assertNull(output);
        assertTrue(presenter.seatAlreadyRemovedCalled);
        assertEquals("seat-r", presenter.seatAlreadyRemovedId.get());
    }

    @Test
    void shouldRejectReservedSeat()
    {
        seatRepo.addSeat(new Seat("seat-r", "room-1", "R-1", SeatStatus.RESERVED));

        var output = useCase.execute(new DeleteSeatUseCase.Request(ADMIN_TOKEN, "seat-r"));

        assertNull(output);
        assertTrue(presenter.seatInUseCalled);
        assertEquals("seat-r", presenter.seatInUseSeatId.get());
        assertEquals(SeatStatus.RESERVED, presenter.seatInUseCurrent.get());
    }

    @Test
    void shouldRejectOccupiedSeat()
    {
        seatRepo.addSeat(new Seat("seat-o", "room-1", "O-1", SeatStatus.OCCUPIED));

        var output = useCase.execute(new DeleteSeatUseCase.Request(ADMIN_TOKEN, "seat-o"));

        assertNull(output);
        assertTrue(presenter.seatInUseCalled);
        assertEquals(SeatStatus.OCCUPIED, presenter.seatInUseCurrent.get());
    }

    @Test
    void shouldRejectWhenActiveReservationsExist()
    {
        seatRepo.addSeat(new Seat("seat-1", "room-1", "A-1", SeatStatus.AVAILABLE));
        activeReservationChecker.hasActiveForSeatResult = true;

        var output = useCase.execute(new DeleteSeatUseCase.Request(ADMIN_TOKEN, "seat-1"));

        assertNull(output);
        assertTrue(presenter.seatHasActiveReservationsCalled);
        assertEquals("seat-1", presenter.seatHasActiveReservationsId.get());
        // Seat status must remain unchanged
        assertEquals(SeatStatus.AVAILABLE, seatRepo.findById("seat-1").get().status());
    }

    @Test
    void shouldAllowWhenOnlyHistoricalReservations()
    {
        seatRepo.addSeat(new Seat("seat-1", "room-1", "A-1", SeatStatus.AVAILABLE));
        activeReservationChecker.hasActiveForSeatResult = false;

        var output = useCase.execute(new DeleteSeatUseCase.Request(ADMIN_TOKEN, "seat-1"));

        assertNotNull(output);
        assertTrue(presenter.deleteSeatSuccessCalled);
    }

    @Test
    void shouldRejectNonAdminUser()
    {
        tokenService.setUserId(STUDENT_ID);
        seatRepo.addSeat(new Seat("seat-1", "room-1", "A-1", SeatStatus.AVAILABLE));

        var output = useCase.execute(new DeleteSeatUseCase.Request(STUDENT_TOKEN, "seat-1"));

        assertNull(output);
        assertTrue(presenter.forbiddenCalled);
    }

    // --- Stubs ---

    static class StubPresenter implements
            DeleteSeatUseCase.Presenter,
            AdminAuthUseCase.Presenter,
            AuthUseCase.Presenter
    {
        boolean deleteSeatSuccessCalled = false;
        AtomicReference<String> deleteSeatSuccessSeatId = new AtomicReference<>();
        boolean seatNotFoundCalled = false;
        AtomicReference<String> seatNotFoundId = new AtomicReference<>();
        boolean seatAlreadyRemovedCalled = false;
        AtomicReference<String> seatAlreadyRemovedId = new AtomicReference<>();
        boolean seatInUseCalled = false;
        AtomicReference<String> seatInUseSeatId = new AtomicReference<>();
        AtomicReference<SeatStatus> seatInUseCurrent = new AtomicReference<>();
        boolean seatHasActiveReservationsCalled = false;
        AtomicReference<String> seatHasActiveReservationsId = new AtomicReference<>();
        boolean forbiddenCalled = false;

        @Override
        public void deleteSeatSuccess(String seatId)
        {
            deleteSeatSuccessCalled = true;
            deleteSeatSuccessSeatId.set(seatId);
        }

        @Override
        public void seatNotFound(String seatId)
        {
            seatNotFoundCalled = true;
            seatNotFoundId.set(seatId);
        }

        @Override
        public void seatAlreadyRemoved(String seatId)
        {
            seatAlreadyRemovedCalled = true;
            seatAlreadyRemovedId.set(seatId);
        }

        @Override
        public void seatInUse(String seatId, SeatStatus current)
        {
            seatInUseCalled = true;
            seatInUseSeatId.set(seatId);
            seatInUseCurrent.set(current);
        }

        @Override
        public void seatHasActiveReservations(String seatId)
        {
            seatHasActiveReservationsCalled = true;
            seatHasActiveReservationsId.set(seatId);
        }

        @Override
        public void forbidden()
        {
            forbiddenCalled = true;
        }

        @Override
        public void invalidToken()
        {
            fail("invalidToken() must not be called");
        }

        @Override
        public void userNotFound()
        {
            fail("userNotFound() must not be called");
        }
    }

    static class StubActiveReservationChecker implements ActiveReservationChecker
    {
        boolean hasActiveForSeatResult = false;

        @Override
        public boolean hasActiveForSeat(String seatId)
        {
            return hasActiveForSeatResult;
        }
    }
}
