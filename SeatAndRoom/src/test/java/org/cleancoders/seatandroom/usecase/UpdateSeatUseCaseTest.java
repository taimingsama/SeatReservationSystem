package org.cleancoders.seatandroom.usecase;

import org.cleancoders.seatandroom.domain.Seat;
import org.cleancoders.seatandroom.domain.SeatStatus;
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

class UpdateSeatUseCaseTest
{
    private static final String ADMIN_ID = "admin-1";
    private static final String ADMIN_TOKEN = "jwt:" + ADMIN_ID + ":admin:ADMIN";
    private static final String STUDENT_ID = "student-1";
    private static final String STUDENT_TOKEN = "jwt:" + STUDENT_ID + ":alice:STUDENT";

    private UpdateSeatUseCase useCase;
    private StubTokenService tokenService;
    private StubUserRepo userRepo;
    private StubSeatRepo seatRepo;
    private StubPresenter presenter;

    @BeforeEach
    void setUp()
    {
        tokenService = new StubTokenService();
        tokenService.setUserId(ADMIN_ID);
        userRepo = new StubUserRepo();
        seatRepo = new StubSeatRepo();
        presenter = new StubPresenter();

        useCase = new UpdateSeatUseCase();
        useCase.tokenService = tokenService;
        useCase.userRepo = userRepo;
        useCase.seatRepo = seatRepo;
        useCase.presenter = presenter;
        ((AdminAuthUseCase<?, ?>) useCase).presenter = presenter;
        ((AuthUseCase<?, ?>) useCase).presenter = presenter;

        userRepo.addUser(new User(ADMIN_ID, "admin", "hashed", UserRole.ADMIN, "Admin", "a@b.com"));
        userRepo.addUser(new User(STUDENT_ID, "alice", "hashed", UserRole.STUDENT, "Alice", "a@b.com"));
    }

    @Test
    void shouldMarkMaintenanceFromAvailable()
    {
        seatRepo.addSeat(new Seat("seat-1", "room-1", "A-1", SeatStatus.AVAILABLE));

        var output = useCase.execute(new UpdateSeatUseCase.Request(
                ADMIN_TOKEN, "seat-1", "MAINTENANCE"));

        assertNotNull(output);
        assertEquals("seat-1", output.seatId());
        assertTrue(presenter.updateSuccessCalled);
        assertEquals(SeatStatus.MAINTENANCE, presenter.updatedSeat.get().status());
    }

    @Test
    void shouldMarkAvailableFromMaintenance()
    {
        seatRepo.addSeat(new Seat("seat-1", "room-1", "A-1", SeatStatus.MAINTENANCE));

        var output = useCase.execute(new UpdateSeatUseCase.Request(
                ADMIN_TOKEN, "seat-1", "AVAILABLE"));

        assertNotNull(output);
        assertTrue(presenter.updateSuccessCalled);
        assertEquals(SeatStatus.AVAILABLE, presenter.updatedSeat.get().status());
    }

    @Test
    void shouldReturnSeatNotFound()
    {
        var output = useCase.execute(new UpdateSeatUseCase.Request(
                ADMIN_TOKEN, "nonexistent", "MAINTENANCE"));

        assertNull(output);
        assertTrue(presenter.seatNotFoundCalled);
        assertEquals("nonexistent", presenter.seatNotFoundId.get());
    }

    @Test
    void shouldRejectInvalidStatusTransition()
    {
        seatRepo.addSeat(new Seat("seat-1", "room-1", "A-1", SeatStatus.RESERVED));

        var output = useCase.execute(new UpdateSeatUseCase.Request(
                ADMIN_TOKEN, "seat-1", "MAINTENANCE"));

        assertNull(output);
        assertTrue(presenter.invalidStatusTransitionCalled);
        assertEquals("seat-1", presenter.transitionSeatId.get());
        assertEquals(SeatStatus.RESERVED, presenter.transitionCurrent.get());
        assertEquals(SeatStatus.MAINTENANCE, presenter.transitionTarget.get());
    }

    @Test
    void shouldRejectInvalidStatusValue()
    {
        seatRepo.addSeat(new Seat("seat-1", "room-1", "A-1", SeatStatus.AVAILABLE));

        var output = useCase.execute(new UpdateSeatUseCase.Request(
                ADMIN_TOKEN, "seat-1", "BROKEN"));

        assertNull(output);
        assertTrue(presenter.invalidStatusCalled);
        assertEquals("seat-1", presenter.invalidStatusSeatId.get());
        assertEquals("BROKEN", presenter.invalidStatusValue.get());
    }

    @Test
    void shouldRejectReservedAsTargetStatus()
    {
        seatRepo.addSeat(new Seat("seat-1", "room-1", "A-1", SeatStatus.AVAILABLE));

        var output = useCase.execute(new UpdateSeatUseCase.Request(
                ADMIN_TOKEN, "seat-1", "RESERVED"));

        assertNull(output);
        assertTrue(presenter.invalidStatusCalled);
    }

    @Test
    void shouldRejectNonAdminUser()
    {
        tokenService.setUserId(STUDENT_ID);
        seatRepo.addSeat(new Seat("seat-1", "room-1", "A-1", SeatStatus.AVAILABLE));

        var output = useCase.execute(new UpdateSeatUseCase.Request(
                STUDENT_TOKEN, "seat-1", "MAINTENANCE"));

        assertNull(output);
        assertTrue(presenter.forbiddenCalled);
    }

    // --- Stubs ---

    static class StubPresenter implements
            UpdateSeatUseCase.Presenter,
            AdminAuthUseCase.Presenter,
            AuthUseCase.Presenter
    {
        boolean updateSuccessCalled = false;
        AtomicReference<Seat> updatedSeat = new AtomicReference<>();
        boolean seatNotFoundCalled = false;
        AtomicReference<String> seatNotFoundId = new AtomicReference<>();
        boolean invalidStatusTransitionCalled = false;
        AtomicReference<String> transitionSeatId = new AtomicReference<>();
        AtomicReference<SeatStatus> transitionCurrent = new AtomicReference<>();
        AtomicReference<SeatStatus> transitionTarget = new AtomicReference<>();
        boolean invalidStatusCalled = false;
        AtomicReference<String> invalidStatusSeatId = new AtomicReference<>();
        AtomicReference<String> invalidStatusValue = new AtomicReference<>();
        boolean forbiddenCalled = false;

        @Override
        public void updateSuccess(Seat seat)
        {
            updateSuccessCalled = true;
            updatedSeat.set(seat);
        }

        @Override
        public void seatNotFound(String seatId)
        {
            seatNotFoundCalled = true;
            seatNotFoundId.set(seatId);
        }

        @Override
        public void invalidStatusTransition(String seatId, SeatStatus current, SeatStatus target)
        {
            invalidStatusTransitionCalled = true;
            transitionSeatId.set(seatId);
            transitionCurrent.set(current);
            transitionTarget.set(target);
        }

        @Override
        public void invalidStatus(String seatId, String status)
        {
            invalidStatusCalled = true;
            invalidStatusSeatId.set(seatId);
            invalidStatusValue.set(status);
        }

        @Override
        public void forbidden() { forbiddenCalled = true; }

        @Override
        public void invalidToken() { fail("invalidToken() must not be called"); }

        @Override
        public void userNotFound() { fail("userNotFound() must not be called"); }
    }
}