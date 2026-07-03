package org.cleancoders.seatandroom.usecase;

import org.cleancoders.common.domain.User;
import org.cleancoders.common.domain.UserRole;
import org.cleancoders.common.usecase.AdminAuthUseCase;
import org.cleancoders.common.usecase.AuthUseCase;
import org.cleancoders.common_test_infrastructure.StubTokenService;
import org.cleancoders.common_test_infrastructure.StubUserRepo;
import org.cleancoders.seatandroom.domain.RoomStatus;
import org.cleancoders.seatandroom.domain.StudyRoom;
import org.cleancoders.seatandroom.test.infrastructure.StubRoomRepo;
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
    private StubPresenter presenter;

    @BeforeEach
    void setUp()
    {
        tokenService = new StubTokenService();
        tokenService.setUserId(ADMIN_ID);
        userRepo = new StubUserRepo();
        roomRepo = new StubRoomRepo();
        presenter = new StubPresenter();

        useCase = new ManageRoomsUseCase();
        useCase.tokenService = tokenService;
        useCase.userRepo = userRepo;
        useCase.roomRepo = roomRepo;
        useCase.presenter = presenter;
        ((AdminAuthUseCase<?, ?>) useCase).presenter = presenter;
        ((AuthUseCase<?, ?>) useCase).presenter = presenter;

        // Default: admin user
        userRepo.addUser(new User(ADMIN_ID, "admin", "hashed", UserRole.ADMIN, "Admin", "a@b.com"));
        userRepo.addUser(new User(STUDENT_ID, "alice", "hashed", UserRole.STUDENT, "Alice", "a@b.com"));
    }

    @Test
    void shouldCreateRoomSuccessfully()
    {
        var output = useCase.execute(new ManageRoomsUseCase.Request(
                ADMIN_TOKEN, "自习室F", "综合楼二楼", 20));

        assertNotNull(output);
        assertNotNull(output.roomId());
        assertTrue(presenter.successCalled);
        assertNotNull(presenter.successRoom.get());
        assertEquals("自习室F", presenter.successRoom.get().name());
        assertEquals("综合楼二楼", presenter.successRoom.get().location());
        assertEquals(20, presenter.successRoom.get().capacity());
        assertEquals(RoomStatus.OPEN, presenter.successRoom.get().status());
    }

    @Test
    void shouldRejectDuplicateName()
    {
        roomRepo.add(new StudyRoom("r-existing", "自习室F", "图书馆一楼", 30, RoomStatus.OPEN));

        var output = useCase.execute(new ManageRoomsUseCase.Request(
                ADMIN_TOKEN, "自习室F", "综合楼二楼", 20));

        assertNull(output);
        assertTrue(presenter.roomNameAlreadyExistsCalled);
        assertEquals("自习室F", presenter.roomNameAlreadyExistsName.get());
    }

    @Test
    void shouldGenerateUniqueIdForEachRoom()
    {
        var output1 = useCase.execute(new ManageRoomsUseCase.Request(
                ADMIN_TOKEN, "自习室X", "一楼", 10));
        var output2 = useCase.execute(new ManageRoomsUseCase.Request(
                ADMIN_TOKEN, "自习室Y", "二楼", 10));

        assertNotNull(output1);
        assertNotNull(output2);
        assertNotEquals(output1.roomId(), output2.roomId());
    }

    @Test
    void shouldSetStatusToOpenByDefault()
    {
        var output = useCase.execute(new ManageRoomsUseCase.Request(
                ADMIN_TOKEN, "自习室Z", "三楼", 15));

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