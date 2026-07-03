package org.cleancoders.seatandroom.usecase;

import org.cleancoders.seatandroom.domain.RoomLayout;
import org.cleancoders.seatandroom.domain.RoomStatus;
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

class ManageRoomsUseCaseTest
{

    private static final String ADMIN_ID = "admin-1";
    private static final String ADMIN_TOKEN = "jwt:" + ADMIN_ID + ":admin:ADMIN";
    private static final String STUDENT_ID = "student-1";
    private static final String STUDENT_TOKEN = "jwt:" + STUDENT_ID + ":alice:STUDENT";

    private ManageRoomsUseCase useCase;
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

        useCase = new ManageRoomsUseCase();
        useCase.tokenService = tokenService;
        useCase.userRepo = userRepo;
        useCase.roomRepo = roomRepo;
        useCase.seatRepo = seatRepo;
        useCase.presenter = presenter;
        ((AdminAuthUseCase<?, ?>) useCase).presenter = presenter;
        ((AuthUseCase<?, ?>) useCase).presenter = presenter;

        userRepo.addUser(new User(ADMIN_ID, "admin", "hashed", UserRole.ADMIN, "Admin", "a@b.com"));
        userRepo.addUser(new User(STUDENT_ID, "alice", "hashed", UserRole.STUDENT, "Alice", "a@b.com"));
    }

    @Test
    void shouldCreateRoomSuccessfully()
    {
        var output = useCase.execute(new ManageRoomsUseCase.Request(
                ADMIN_TOKEN, "自习室F", "综合楼二楼", "SMALL"));

        assertNotNull(output);
        assertNotNull(output.roomId());
        assertTrue(presenter.successCalled);
        assertNotNull(presenter.successRoom.get());
        assertEquals("自习室F", presenter.successRoom.get().name());
        assertEquals("综合楼二楼", presenter.successRoom.get().location());
        assertEquals(RoomLayout.SMALL, presenter.successRoom.get().layout());
        assertEquals(RoomStatus.OPEN, presenter.successRoom.get().status());
    }

    @Test
    void shouldAutoGenerateSeats()
    {
        var output = useCase.execute(new ManageRoomsUseCase.Request(
                ADMIN_TOKEN, "自习室Z", "三楼", "SMALL"));

        assertNotNull(output);
        // SMALL layout = 40 seats
        var seats = seatRepo.findByRoomId(output.roomId());
        assertEquals(40, seats.size());
        // Seats should be 1..40
        for (int i = 1; i <= 40; i++)
        {
            assertTrue(seatRepo.findByRoomIdAndSeatId(output.roomId(), i).isPresent());
        }
    }

    @Test
    void shouldRejectDuplicateName()
    {
        roomRepo.add(new StudyRoom("r-existing", "自习室F", "图书馆一楼", RoomLayout.SMALL, RoomStatus.OPEN));

        var output = useCase.execute(new ManageRoomsUseCase.Request(
                ADMIN_TOKEN, "自习室F", "综合楼二楼", "SMALL"));

        assertNull(output);
        assertTrue(presenter.roomNameAlreadyExistsCalled);
        assertEquals("自习室F", presenter.roomNameAlreadyExistsName.get());
    }

    @Test
    void shouldRejectInvalidLayout()
    {
        var output = useCase.execute(new ManageRoomsUseCase.Request(
                ADMIN_TOKEN, "自习室X", "一楼", "INVALID"));

        assertNull(output);
        assertTrue(presenter.invalidLayoutCalled);
        assertEquals("INVALID", presenter.invalidLayoutValue.get());
    }

    @Test
    void shouldGenerateUniqueIdForEachRoom()
    {
        var output1 = useCase.execute(new ManageRoomsUseCase.Request(
                ADMIN_TOKEN, "自习室X", "一楼", "SMALL"));
        var output2 = useCase.execute(new ManageRoomsUseCase.Request(
                ADMIN_TOKEN, "自习室Y", "二楼", "MEDIUM"));

        assertNotNull(output1);
        assertNotNull(output2);
        assertNotEquals(output1.roomId(), output2.roomId());
    }

    @Test
    void shouldSetStatusToOpenByDefault()
    {
        var output = useCase.execute(new ManageRoomsUseCase.Request(
                ADMIN_TOKEN, "自习室Z", "三楼", "LARGE"));

        assertNotNull(output);
        assertEquals(RoomStatus.OPEN, presenter.successRoom.get().status());
    }

    // --- Stubs ---

    static class StubPresenter implements
            ManageRoomsUseCase.Presenter,
            AdminAuthUseCase.Presenter,
            AuthUseCase.Presenter
    {
        boolean successCalled = false;
        AtomicReference<StudyRoom> successRoom = new AtomicReference<>();
        boolean roomNameAlreadyExistsCalled = false;
        AtomicReference<String> roomNameAlreadyExistsName = new AtomicReference<>();
        boolean invalidLayoutCalled = false;
        AtomicReference<String> invalidLayoutValue = new AtomicReference<>();

        @Override
        public void success(StudyRoom room)
        {
            successCalled = true;
            successRoom.set(room);
        }

        @Override
        public void roomNameAlreadyExists(String name)
        {
            roomNameAlreadyExistsCalled = true;
            roomNameAlreadyExistsName.set(name);
        }

        @Override
        public void invalidLayout(String layout)
        {
            invalidLayoutCalled = true;
            invalidLayoutValue.set(layout);
        }

        @Override
        public void forbidden()
        {
            fail("forbidden() must not be called — token validation is not under test");
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
