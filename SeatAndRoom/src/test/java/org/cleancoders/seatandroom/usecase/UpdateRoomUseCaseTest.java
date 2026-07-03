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

class UpdateRoomUseCaseTest
{

    private static final String ADMIN_ID = "admin-1";
    private static final String ADMIN_TOKEN = "jwt:" + ADMIN_ID + ":admin:ADMIN";
    private static final String STUDENT_ID = "student-1";
    private static final String STUDENT_TOKEN = "jwt:" + STUDENT_ID + ":alice:STUDENT";

    private UpdateRoomUseCase useCase;
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

        useCase = new UpdateRoomUseCase();
        useCase.tokenService = tokenService;
        useCase.userRepo = userRepo;
        useCase.roomRepo = roomRepo;
        useCase.presenter = presenter;
        ((AdminAuthUseCase<?, ?>) useCase).presenter = presenter;
        ((AuthUseCase<?, ?>) useCase).presenter = presenter;

        userRepo.addUser(new User(ADMIN_ID, "admin", "hashed", UserRole.ADMIN, "Admin", "a@b.com"));
        userRepo.addUser(new User(STUDENT_ID, "alice", "hashed", UserRole.STUDENT, "Alice", "a@b.com"));

        roomRepo.add(new StudyRoom("room-1", "自习室A", "图书馆一楼", 30, RoomStatus.OPEN));
        roomRepo.add(new StudyRoom("room-2", "自习室B", "图书馆二楼", 20, RoomStatus.OPEN));
    }

    @Test
    void shouldUpdateRoomSuccessfully()
    {
        var output = useCase.execute(new UpdateRoomUseCase.Request(
                ADMIN_TOKEN, "room-1", "自习室A-改", "图书馆一楼东", 35));

        assertNotNull(output);
        assertEquals("room-1", output.roomId());
        assertTrue(presenter.updateSuccessCalled);
        assertEquals("自习室A-改", presenter.updatedRoom.get().name());
        assertEquals("图书馆一楼东", presenter.updatedRoom.get().location());
        assertEquals(35, presenter.updatedRoom.get().capacity());
    }

    @Test
    void shouldKeepSameNameWhenOnlyUpdatingOtherFields()
    {
        var output = useCase.execute(new UpdateRoomUseCase.Request(
                ADMIN_TOKEN, "room-1", "自习室A", "新位置", 40));

        assertNotNull(output);
        assertTrue(presenter.updateSuccessCalled);
        assertEquals("自习室A", presenter.updatedRoom.get().name());
        assertEquals("新位置", presenter.updatedRoom.get().location());
        assertEquals(40, presenter.updatedRoom.get().capacity());
    }

    @Test
    void shouldRejectNonAdminUser()
    {
        tokenService.setUserId(STUDENT_ID);

        var output = useCase.execute(new UpdateRoomUseCase.Request(
                STUDENT_TOKEN, "room-1", "自习室F", "综合楼二楼", 20));

        assertNull(output);
        assertTrue(presenter.forbiddenCalled);
    }

    @Test
    void shouldReturnRoomNotFound()
    {
        var output = useCase.execute(new UpdateRoomUseCase.Request(
                ADMIN_TOKEN, "nonexistent", "自习室X", "一楼", 10));

        assertNull(output);
        assertTrue(presenter.roomNotFoundCalled);
        assertEquals("nonexistent", presenter.roomNotFoundId.get());
    }

    @Test
    void shouldRejectDuplicateNameOnDifferentRoom()
    {
        var output = useCase.execute(new UpdateRoomUseCase.Request(
                ADMIN_TOKEN, "room-1", "自习室B", "图书馆一楼", 30));

        assertNull(output);
        assertTrue(presenter.roomNameAlreadyExistsCalled);
        assertEquals("自习室B", presenter.roomNameAlreadyExistsName.get());
    }

    @Test
    void shouldAllowSameNameOnSameRoom()
    {
        var output = useCase.execute(new UpdateRoomUseCase.Request(
                ADMIN_TOKEN, "room-1", "自习室B", "新位置", 25));

        assertNull(output);
        assertTrue(presenter.roomNameAlreadyExistsCalled);
    }

    // --- Stubs ---

    static class StubPresenter implements
            UpdateRoomUseCase.Presenter,
            AdminAuthUseCase.Presenter,
            AuthUseCase.Presenter
    {
        boolean updateSuccessCalled = false;
        AtomicReference<StudyRoom> updatedRoom = new AtomicReference<>();
        boolean roomNotFoundCalled = false;
        AtomicReference<String> roomNotFoundId = new AtomicReference<>();
        boolean roomNameAlreadyExistsCalled = false;
        AtomicReference<String> roomNameAlreadyExistsName = new AtomicReference<>();
        boolean forbiddenCalled = false;

        @Override
        public void updateSuccess(StudyRoom room)
        {
            updateSuccessCalled = true;
            updatedRoom.set(room);
        }

        @Override
        public void roomNotFound(String roomId)
        {
            roomNotFoundCalled = true;
            roomNotFoundId.set(roomId);
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
}