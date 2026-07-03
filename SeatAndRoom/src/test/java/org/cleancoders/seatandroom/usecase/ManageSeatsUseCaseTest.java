package org.cleancoders.seatandroom.usecase;

import org.cleancoders.seatandroom.domain.RoomStatus;
import org.cleancoders.seatandroom.domain.Seat;
import org.cleancoders.seatandroom.domain.SeatStatus;
import org.cleancoders.seatandroom.domain.StudyRoom;
import org.cleancoders.seatandroom.test.infrastructure.StubRoomRepo;
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

class ManageSeatsUseCaseTest
{

    private static final String ADMIN_ID = "admin-1";
    private static final String ADMIN_TOKEN = "jwt:" + ADMIN_ID + ":admin:ADMIN";
    private static final String STUDENT_ID = "student-1";
    private static final String STUDENT_TOKEN = "jwt:" + STUDENT_ID + ":alice:STUDENT";

    private ManageSeatsUseCase useCase;
    private StubTokenService tokenService;
    private StubUserRepo userRepo;
    private StubRoomRepo roomRepo;
    private StubSeatRepo seatRepo;
    private StubPresenter presenter;

    @BeforeEach
    void setUp()
    {
        tokenService = new StubTokenService();
        tokenService.setUserId(ADMIN_ID);
        userRepo = new StubUserRepo();
        roomRepo = new StubRoomRepo();
        seatRepo = new StubSeatRepo();
        presenter = new StubPresenter();

        useCase = new ManageSeatsUseCase();
        useCase.tokenService = tokenService;
        useCase.userRepo = userRepo;
        useCase.seatRepo = seatRepo;
        useCase.roomRepo = roomRepo;
        useCase.presenter = presenter;
        ((AdminAuthUseCase<?, ?>) useCase).presenter = presenter;
        ((AuthUseCase<?, ?>) useCase).presenter = presenter;

        // Default: admin user
        userRepo.addUser(new User(ADMIN_ID, "admin", "hashed", UserRole.ADMIN, "Admin", "a@b.com"));
        userRepo.addUser(new User(STUDENT_ID, "alice", "hashed", UserRole.STUDENT, "Alice", "a@b.com"));

        // Default: a study room exists for seat creation
        roomRepo.add(new StudyRoom("room-1", "自习室A", "图书馆一楼", 30, RoomStatus.OPEN));
    }

    @Test
    void shouldCreateSeatSuccessfully()
    {
        var output = useCase.execute(new ManageSeatsUseCase.Request(
                ADMIN_TOKEN, "room-1", "A-9"));

        assertNotNull(output);
        assertNotNull(output.seatId());
        assertTrue(presenter.successCalled);
        assertNotNull(presenter.successSeat.get());
        assertEquals("room-1", presenter.successSeat.get().roomId());
        assertEquals("A-9", presenter.successSeat.get().seatNumber());
        assertEquals(SeatStatus.AVAILABLE, presenter.successSeat.get().status());
    }

    @Test
    void shouldRejectRoomNotFound()
    {
        var output = useCase.execute(new ManageSeatsUseCase.Request(
                ADMIN_TOKEN, "nonexistent", "A-9"));

        assertNull(output);
        assertTrue(presenter.roomNotFoundCalled);
        assertEquals("nonexistent", presenter.roomNotFoundId.get());
    }

    @Test
    void shouldRejectDuplicateSeatNumber()
    {
        seatRepo.addSeat(new Seat("seat-existing", "room-1", "A-1", SeatStatus.AVAILABLE));

        var output = useCase.execute(new ManageSeatsUseCase.Request(
                ADMIN_TOKEN, "room-1", "A-1"));

        assertNull(output);
        assertTrue(presenter.seatNumberAlreadyExistsCalled);
        assertEquals("room-1", presenter.duplicateRoomId.get());
        assertEquals("A-1", presenter.duplicateSeatNumber.get());
    }

    @Test
    void shouldAllowSameSeatNumberInDifferentRooms()
    {
        roomRepo.add(new StudyRoom("room-2", "自习室B", "图书馆二楼", 20, RoomStatus.OPEN));
        seatRepo.addSeat(new Seat("seat-existing", "room-1", "A-1", SeatStatus.AVAILABLE));

        var output = useCase.execute(new ManageSeatsUseCase.Request(
                ADMIN_TOKEN, "room-2", "A-1"));

        assertNotNull(output);
        assertTrue(presenter.successCalled);
        assertEquals("room-2", presenter.successSeat.get().roomId());
        assertEquals("A-1", presenter.successSeat.get().seatNumber());
    }

    @Test
    void shouldGenerateUniqueIdForEachSeat()
    {
        var output1 = useCase.execute(new ManageSeatsUseCase.Request(
                ADMIN_TOKEN, "room-1", "A-9"));
        var output2 = useCase.execute(new ManageSeatsUseCase.Request(
                ADMIN_TOKEN, "room-1", "A-10"));

        assertNotNull(output1);
        assertNotNull(output2);
        assertNotEquals(output1.seatId(), output2.seatId());
    }

    @Test
    void shouldSetStatusToAvailableByDefault()
    {
        useCase.execute(new ManageSeatsUseCase.Request(
                ADMIN_TOKEN, "room-1", "A-9"));

        assertEquals(SeatStatus.AVAILABLE, presenter.successSeat.get().status());
    }

    @Test
    void shouldRejectStudentRole()
    {
        tokenService.setUserId(STUDENT_ID);

        var output = useCase.execute(new ManageSeatsUseCase.Request(
                STUDENT_TOKEN, "room-1", "A-9"));

        assertNull(output);
        assertTrue(presenter.forbiddenCalled);
    }

    // --- Stubs ---

    static class StubPresenter implements
            ManageSeatsUseCase.Presenter,
            AdminAuthUseCase.Presenter,
            AuthUseCase.Presenter
    {
        boolean successCalled = false;
        AtomicReference<Seat> successSeat = new AtomicReference<>();
        boolean roomNotFoundCalled = false;
        AtomicReference<String> roomNotFoundId = new AtomicReference<>();
        boolean seatNumberAlreadyExistsCalled = false;
        AtomicReference<String> duplicateRoomId = new AtomicReference<>();
        AtomicReference<String> duplicateSeatNumber = new AtomicReference<>();
        boolean forbiddenCalled = false;

        @Override
        public void success(Seat seat)
        {
            successCalled = true;
            successSeat.set(seat);
        }

        @Override
        public void roomNotFound(String roomId)
        {
            roomNotFoundCalled = true;
            roomNotFoundId.set(roomId);
        }

        @Override
        public void seatNumberAlreadyExists(String roomId, String seatNumber)
        {
            seatNumberAlreadyExistsCalled = true;
            duplicateRoomId.set(roomId);
            duplicateSeatNumber.set(seatNumber);
        }

        @Override
        public void forbidden()
        {
            forbiddenCalled = true;
        }

        @Override
        public void invalidToken()
        {
            fail("invalidToken() must not be called — token validation is not under test");
        }

        @Override
        public void userNotFound()
        {
            fail("userNotFound() must not be called — token validation is not under test");
        }
    }
}