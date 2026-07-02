package org.cleancoders.seatandroom.usecase;

import org.cleancoders.common_reservation_seatAndRoom.domain.Seat;
import org.cleancoders.common_reservation_seatAndRoom.domain.SeatStatus;
import org.cleancoders.common_reservation_seatandroom_test_infrastructure.StubSeatRepo;
import org.cleancoders.seatandroom.domain.RoomStatus;
import org.cleancoders.seatandroom.domain.StudyRoom;
import org.cleancoders.seatandroom.test.infrastructure.StubRoomRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ListSeatsUseCaseTest
{

    private ListSeatsUseCase useCase;
    private StubRoomRepo roomRepo;
    private StubSeatRepo seatRepo;
    private StubPresenter presenter;

    @BeforeEach
    void setUp()
    {
        roomRepo = new StubRoomRepo();
        seatRepo = new StubSeatRepo();
        presenter = new StubPresenter();
        useCase = new ListSeatsUseCase();
        useCase.roomRepo = roomRepo;
        useCase.seatRepo = seatRepo;
        useCase.presenter = presenter;
    }

    @Test
    void shouldReturnSeatsAndPresentThem()
    {
        StudyRoom room = new StudyRoom("room-1", "自习室A", "图书馆一楼", 30, RoomStatus.OPEN);
        roomRepo.add(room);
        Seat s1 = new Seat("seat-1", "room-1", "A-1", SeatStatus.AVAILABLE);
        Seat s2 = new Seat("seat-2", "room-1", "A-2", SeatStatus.RESERVED);
        Seat s3 = new Seat("seat-3", "room-1", "A-3", SeatStatus.OCCUPIED);
        seatRepo.save(s1);
        seatRepo.save(s2);
        seatRepo.save(s3);

        var output = useCase.execute(new ListSeatsUseCase.Request("room-1"));

        assertEquals(List.of(s1, s2, s3), output.seats());
        assertEquals(room, presenter.presentedRoom.get());
        assertEquals(List.of(s1, s2, s3), presenter.presentedSeats.get());
    }

    @Test
    void shouldReturnEmptyListWhenRoomHasNoSeats()
    {
        StudyRoom room = new StudyRoom("room-1", "自习室A", "图书馆一楼", 30, RoomStatus.OPEN);
        roomRepo.add(room);

        var output = useCase.execute(new ListSeatsUseCase.Request("room-1"));

        assertTrue(output.seats().isEmpty());
        assertTrue(presenter.presentedSeats.get().isEmpty());
    }

    @Test
    void shouldCallRoomNotFoundWhenRoomDoesNotExist()
    {
        var output = useCase.execute(new ListSeatsUseCase.Request("nonexistent"));

        assertTrue(output.seats().isEmpty());
        assertEquals("nonexistent", presenter.roomNotFoundId.get());
    }

    @Test
    void shouldReturnSeatsForCorrectRoomOnly()
    {
        StudyRoom room1 = new StudyRoom("room-1", "自习室A", "图书馆一楼", 30, RoomStatus.OPEN);
        StudyRoom room2 = new StudyRoom("room-2", "自习室B", "图书馆二楼", 20, RoomStatus.OPEN);
        roomRepo.add(room1, room2);
        Seat s1 = new Seat("seat-1", "room-1", "A-1", SeatStatus.AVAILABLE);
        Seat s2 = new Seat("seat-9", "room-2", "B-1", SeatStatus.AVAILABLE);
        seatRepo.save(s1);
        seatRepo.save(s2);

        var output = useCase.execute(new ListSeatsUseCase.Request("room-1"));

        assertEquals(1, output.seats().size());
        assertEquals("seat-1", output.seats().get(0).id());
    }

    @Test
    void shouldIncludeAllSeatStatuses()
    {
        StudyRoom room = new StudyRoom("room-3", "自习室C", "教学楼三楼", 15, RoomStatus.OPEN);
        roomRepo.add(room);
        Seat av = new Seat("s1", "room-3", "C-1", SeatStatus.AVAILABLE);
        Seat rv = new Seat("s2", "room-3", "C-2", SeatStatus.RESERVED);
        Seat oc = new Seat("s3", "room-3", "C-3", SeatStatus.OCCUPIED);
        Seat mt = new Seat("s4", "room-3", "C-4", SeatStatus.MAINTENANCE);
        seatRepo.save(av);
        seatRepo.save(rv);
        seatRepo.save(oc);
        seatRepo.save(mt);

        var output = useCase.execute(new ListSeatsUseCase.Request("room-3"));

        assertEquals(4, output.seats().size());
        assertTrue(output.seats().stream().anyMatch(s -> s.status() == SeatStatus.AVAILABLE));
        assertTrue(output.seats().stream().anyMatch(s -> s.status() == SeatStatus.RESERVED));
        assertTrue(output.seats().stream().anyMatch(s -> s.status() == SeatStatus.OCCUPIED));
        assertTrue(output.seats().stream().anyMatch(s -> s.status() == SeatStatus.MAINTENANCE));
    }

    // --- Stubs ---

    static class StubPresenter implements ListSeatsUseCase.Presenter
    {
        final AtomicReference<StudyRoom> presentedRoom = new AtomicReference<>();
        final AtomicReference<List<Seat>> presentedSeats = new AtomicReference<>();
        final AtomicReference<String> roomNotFoundId = new AtomicReference<>();

        @Override
        public void presentSeats(StudyRoom room, List<Seat> seats)
        {
            presentedRoom.set(room);
            presentedSeats.set(seats);
        }

        @Override
        public void roomNotFound(String roomId)
        {
            roomNotFoundId.set(roomId);
        }
    }
}