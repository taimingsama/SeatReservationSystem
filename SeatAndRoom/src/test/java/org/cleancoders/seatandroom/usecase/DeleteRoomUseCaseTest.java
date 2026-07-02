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

class DeleteRoomUseCaseTest
{

    private static final String ADMIN_ID = "admin-1";
    private static final String ADMIN_TOKEN = "jwt:" + ADMIN_ID + ":admin:ADMIN";
    private static final String STUDENT_ID = "student-1";
    private static final String STUDENT_TOKEN = "jwt:" + STUDENT_ID + ":alice:STUDENT";

    private DeleteRoomUseCase useCase;
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

        useCase = new DeleteRoomUseCase();
        useCase.tokenService = tokenService;
        useCase.userRepo = userRepo;
        useCase.roomRepo = roomRepo;
        useCase.presenter = presenter;
        ((AdminAuthUseCase<?, ?>) useCase).presenter = presenter;
        ((AuthUseCase<?, ?>) useCase).presenter = presenter;

        userRepo.addUser(new User(ADMIN_ID, "admin", "hashed", UserRole.ADMIN, "Admin", "a@b.com"));
        userRepo.addUser(new User(STUDENT_ID, "alice", "hashed", UserRole.STUDENT, "Alice", "a@b.com"));

        roomRepo.add(new StudyRoom("room-1", "自习室A", "图书馆一楼", 30, RoomStatus.OPEN));
    }

    @Test
    void shouldDeleteRoomSuccessfully()
    {
        var output = useCase.execute(new DeleteRoomUseCase.Request(ADMIN_TOKEN, "room-1"));

        assertNotNull(output);
        assertEquals("room-1", output.roomId());
        assertTrue(presenter.deleteSuccessCalled);
        assertEquals("room-1", presenter.deleteSuccessRoomId.get());
        assertEquals(RoomStatus.CLOSED, roomRepo.findById("room-1").get().status());
    }

    @Test
    void shouldRejectNonAdminUser()
    {
        tokenService.setUserId(STUDENT_ID);

        var output = useCase.execute(new DeleteRoomUseCase.Request(STUDENT_TOKEN, "room-1"));

        assertNull(output);
        assertTrue(presenter.forbiddenCalled);
    }

    @Test
    void shouldReturnRoomNotFound()
    {
        var output = useCase.execute(new DeleteRoomUseCase.Request(ADMIN_TOKEN, "nonexistent"));

        assertNull(output);
        assertTrue(presenter.roomNotFoundCalled);
        assertEquals("nonexistent", presenter.roomNotFoundId.get());
    }

    @Test
    void shouldRejectAlreadyClosedRoom()
    {
        roomRepo.add(new StudyRoom("room-closed", "已关闭", "三楼", 10, RoomStatus.CLOSED));

        var output = useCase.execute(new DeleteRoomUseCase.Request(ADMIN_TOKEN, "room-closed"));

        assertNull(output);
        assertTrue(presenter.roomAlreadyClosedCalled);
        assertEquals("room-closed", presenter.roomAlreadyClosedId.get());
    }

    // --- Stubs ---

    static class StubPresenter implements
            DeleteRoomUseCase.Presenter,
            AdminAuthUseCase.Presenter,
            AuthUseCase.Presenter
    {
        boolean deleteSuccessCalled = false;
        AtomicReference<String> deleteSuccessRoomId = new AtomicReference<>();
        boolean roomNotFoundCalled = false;
        AtomicReference<String> roomNotFoundId = new AtomicReference<>();
        boolean roomAlreadyClosedCalled = false;
        AtomicReference<String> roomAlreadyClosedId = new AtomicReference<>();
        boolean forbiddenCalled = false;

        @Override
        public void deleteSuccess(String roomId)
        {
            deleteSuccessCalled = true;
            deleteSuccessRoomId.set(roomId);
        }

        @Override
        public void roomNotFound(String roomId)
        {
            roomNotFoundCalled = true;
            roomNotFoundId.set(roomId);
        }

        @Override
        public void roomAlreadyClosed(String roomId)
        {
            roomAlreadyClosedCalled = true;
            roomAlreadyClosedId.set(roomId);
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