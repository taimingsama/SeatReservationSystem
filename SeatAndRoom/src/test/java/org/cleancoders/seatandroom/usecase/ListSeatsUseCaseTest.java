package org.cleancoders.seatandroom.usecase;

import org.cleancoders.seatandroom.domain.RoomLayout;
import org.cleancoders.seatandroom.domain.RoomStatus;
import org.cleancoders.seatandroom.domain.Seat;
import org.cleancoders.seatandroom.domain.SeatStatus;
import org.cleancoders.seatandroom.domain.StudyRoom;
import org.cleancoders.seatandroom.domain.TimeSlot;
import org.cleancoders.seatandroom.outbound.ActiveReservationChecker;
import org.cleancoders.seatandroom.outbound.TimeSlotRepository;
import org.cleancoders.seatandroom.test.infrastructure.StubRoomRepo;
import org.cleancoders.seatandroom.test.infrastructure.StubSeatRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class ListSeatsUseCaseTest
{

    private ListSeatsUseCase useCase;
    private StubRoomRepo roomRepo;
    private StubSeatRepo seatRepo;
    private StubTimeSlotRepo timeSlotRepo;
    private StubPresenter presenter;
    private StubActiveReservationChecker checker;

    @BeforeEach
    void setUp()
    {
        roomRepo = new StubRoomRepo();
        seatRepo = new StubSeatRepo();
        timeSlotRepo = new StubTimeSlotRepo();
        presenter = new StubPresenter();
        checker = new StubActiveReservationChecker();
        useCase = new ListSeatsUseCase();
        useCase.roomRepo = roomRepo;
        useCase.seatRepo = seatRepo;
        useCase.timeSlotRepo = timeSlotRepo;
        useCase.activeReservationChecker = checker;
        useCase.presenter = presenter;
    }

    // --- basic tests (no time slot) ---

    @Test
    void shouldReturnSeatsAndPresentThem()
    {
        StudyRoom room = new StudyRoom("room-1", "自习室A", "图书馆一楼", RoomLayout.SMALL, RoomStatus.OPEN);
        roomRepo.add(room);
        Seat s1 = new Seat(1, "room-1", SeatStatus.AVAILABLE);
        Seat s2 = new Seat(2, "room-1", SeatStatus.RESERVED);
        Seat s3 = new Seat(3, "room-1", SeatStatus.OCCUPIED);
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
        StudyRoom room = new StudyRoom("room-1", "自习室A", "图书馆一楼", RoomLayout.SMALL, RoomStatus.OPEN);
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
        StudyRoom room1 = new StudyRoom("room-1", "自习室A", "图书馆一楼", RoomLayout.SMALL, RoomStatus.OPEN);
        StudyRoom room2 = new StudyRoom("room-2", "自习室B", "图书馆二楼", RoomLayout.SMALL, RoomStatus.OPEN);
        roomRepo.add(room1, room2);
        Seat s1 = new Seat(1, "room-1", SeatStatus.AVAILABLE);
        Seat s2 = new Seat(9, "room-2", SeatStatus.AVAILABLE);
        seatRepo.save(s1);
        seatRepo.save(s2);

        var output = useCase.execute(new ListSeatsUseCase.Request("room-1"));

        assertEquals(1, output.seats().size());
        assertEquals(1, output.seats().get(0).id());
    }

    @Test
    void shouldIncludeAllSeatStatuses()
    {
        StudyRoom room = new StudyRoom("room-3", "自习室C", "教学楼三楼", RoomLayout.SMALL, RoomStatus.OPEN);
        roomRepo.add(room);
        Seat av = new Seat(1, "room-3", SeatStatus.AVAILABLE);
        Seat rv = new Seat(2, "room-3", SeatStatus.RESERVED);
        Seat oc = new Seat(3, "room-3", SeatStatus.OCCUPIED);
        Seat mt = new Seat(4, "room-3", SeatStatus.MAINTENANCE);
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

    // --- time-slot-aware tests ---

    @Test
    void shouldMarkAvailableSeatAsReservedWhenTimeSlotProvided()
    {
        StudyRoom room = new StudyRoom("room-1", "自习室A", "图书馆一楼", RoomLayout.SMALL, RoomStatus.OPEN);
        roomRepo.add(room);
        Seat s1 = new Seat(1, "room-1", SeatStatus.AVAILABLE);
        Seat s2 = new Seat(2, "room-1", SeatStatus.AVAILABLE);
        Seat s3 = new Seat(3, "room-1", SeatStatus.MAINTENANCE);
        seatRepo.save(s1);
        seatRepo.save(s2);
        seatRepo.save(s3);
        timeSlotRepo.addTimeSlot(new TimeSlot("ts-1", "08:00", "12:00", "上午 08:00-12:00"));

        checker.markReserved("room-1", 1, "ts-1", LocalDate.of(2026, 7, 4));

        // future slot (now 07:00, slot starts 08:00)
        useCase = withFixedTime(LocalDateTime.of(2026, 7, 4, 7, 0));

        var output = useCase.execute(new ListSeatsUseCase.Request(
                "room-1", "ts-1", LocalDate.of(2026, 7, 4)));

        assertEquals(3, output.seats().size());
        assertEquals(SeatStatus.RESERVED, output.seats().get(0).status()); // AVAILABLE → RESERVED
        assertEquals(SeatStatus.AVAILABLE, output.seats().get(1).status()); // no booking
        assertEquals(SeatStatus.MAINTENANCE, output.seats().get(2).status()); // unchanged
    }

    @Test
    void shouldReturnOccupiedForCheckedInDuringCurrentSlot()
    {
        StudyRoom room = new StudyRoom("room-1", "自习室A", "图书馆一楼", RoomLayout.SMALL, RoomStatus.OPEN);
        roomRepo.add(room);
        Seat s1 = new Seat(1, "room-1", SeatStatus.AVAILABLE);
        seatRepo.save(s1);
        timeSlotRepo.addTimeSlot(new TimeSlot("ts-1", "08:00", "12:00", "上午 08:00-12:00"));

        checker.markCheckedIn("room-1", 1, "ts-1", LocalDate.of(2026, 7, 4));

        // current slot (now 10:00, slot 08:00-12:00)
        useCase = withFixedTime(LocalDateTime.of(2026, 7, 4, 10, 0));

        var output = useCase.execute(new ListSeatsUseCase.Request(
                "room-1", "ts-1", LocalDate.of(2026, 7, 4)));

        assertEquals(1, output.seats().size());
        assertEquals(SeatStatus.OCCUPIED, output.seats().get(0).status());
    }

    @Test
    void shouldReturnReservedForCheckedInDuringFutureSlot()
    {
        StudyRoom room = new StudyRoom("room-1", "自习室A", "图书馆一楼", RoomLayout.SMALL, RoomStatus.OPEN);
        roomRepo.add(room);
        Seat s1 = new Seat(1, "room-1", SeatStatus.AVAILABLE);
        seatRepo.save(s1);
        timeSlotRepo.addTimeSlot(new TimeSlot("ts-2", "13:00", "17:00", "下午 13:00-17:00"));

        checker.markCheckedIn("room-1", 1, "ts-2", LocalDate.of(2026, 7, 4));

        // future slot (now 10:00, slot starts 13:00)
        useCase = withFixedTime(LocalDateTime.of(2026, 7, 4, 10, 0));

        var output = useCase.execute(new ListSeatsUseCase.Request(
                "room-1", "ts-2", LocalDate.of(2026, 7, 4)));

        assertEquals(1, output.seats().size());
        // CHECKED_IN in future → mapped to RESERVED (no OCCUPIED in future)
        assertEquals(SeatStatus.RESERVED, output.seats().get(0).status());
    }

    @Test
    void shouldRejectPastTimeSlot()
    {
        StudyRoom room = new StudyRoom("room-1", "自习室A", "图书馆一楼", RoomLayout.SMALL, RoomStatus.OPEN);
        roomRepo.add(room);
        timeSlotRepo.addTimeSlot(new TimeSlot("ts-1", "08:00", "12:00", "上午 08:00-12:00"));

        // past slot (now 14:00, slot ended 12:00)
        useCase = withFixedTime(LocalDateTime.of(2026, 7, 4, 14, 0));

        var output = useCase.execute(new ListSeatsUseCase.Request(
                "room-1", "ts-1", LocalDate.of(2026, 7, 4)));

        assertTrue(output.seats().isEmpty());
        assertEquals("ts-1", presenter.pastTimeSlotId.get());
    }

    @Test
    void shouldKeepMaintenanceRegardlessOfTimeSlot()
    {
        StudyRoom room = new StudyRoom("room-1", "自习室A", "图书馆一楼", RoomLayout.SMALL, RoomStatus.OPEN);
        roomRepo.add(room);
        Seat s1 = new Seat(1, "room-1", SeatStatus.MAINTENANCE);
        seatRepo.save(s1);
        timeSlotRepo.addTimeSlot(new TimeSlot("ts-1", "08:00", "12:00", "上午 08:00-12:00"));

        // current slot (now 10:00)
        useCase = withFixedTime(LocalDateTime.of(2026, 7, 4, 10, 0));

        var output = useCase.execute(new ListSeatsUseCase.Request(
                "room-1", "ts-1", LocalDate.of(2026, 7, 4)));

        assertEquals(1, output.seats().size());
        assertEquals(SeatStatus.MAINTENANCE, output.seats().get(0).status());
    }

    // --- helpers ---

    private ListSeatsUseCase withFixedTime(LocalDateTime fixedTime)
    {
        ListSeatsUseCase uc = new ListSeatsUseCase()
        {
            @Override
            protected LocalDateTime getCurrentTime()
            {
                return fixedTime;
            }
        };
        uc.roomRepo = roomRepo;
        uc.seatRepo = seatRepo;
        uc.timeSlotRepo = timeSlotRepo;
        uc.activeReservationChecker = checker;
        uc.presenter = presenter;
        return uc;
    }

    // --- Stubs ---

    static class StubActiveReservationChecker implements ActiveReservationChecker
    {
        private final java.util.Set<String> reserved = new java.util.HashSet<>();
        private final java.util.Set<String> checkedIn = new java.util.HashSet<>();

        void markReserved(String roomId, int seatId, String timeSlotId, LocalDate date)
        {
            reserved.add(roomId + ":" + seatId + ":" + timeSlotId + ":" + date);
        }

        void markCheckedIn(String roomId, int seatId, String timeSlotId, LocalDate date)
        {
            checkedIn.add(roomId + ":" + seatId + ":" + timeSlotId + ":" + date);
        }

        @Override
        public boolean hasActiveForSeat(String roomId, int seatId)
        {
            return false;
        }

        @Override
        public boolean isReservedForTimeSlot(String roomId, int seatId, String timeSlotId, LocalDate date)
        {
            String key = roomId + ":" + seatId + ":" + timeSlotId + ":" + date;
            return reserved.contains(key) || checkedIn.contains(key);
        }

        @Override
        public boolean isCheckedInForTimeSlot(String roomId, int seatId, String timeSlotId, LocalDate date)
        {
            return checkedIn.contains(roomId + ":" + seatId + ":" + timeSlotId + ":" + date);
        }
    }

    static class StubTimeSlotRepo implements TimeSlotRepository
    {
        private final Map<String, TimeSlot> store = new HashMap<>();

        void addTimeSlot(TimeSlot ts)
        {
            store.put(ts.id(), ts);
        }

        @Override
        public Optional<TimeSlot> findById(String id)
        {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public List<TimeSlot> findAll()
        {
            return List.copyOf(store.values());
        }
    }

    static class StubPresenter implements ListSeatsUseCase.Presenter
    {
        final AtomicReference<StudyRoom> presentedRoom = new AtomicReference<>();
        final AtomicReference<List<Seat>> presentedSeats = new AtomicReference<>();
        final AtomicReference<String> roomNotFoundId = new AtomicReference<>();
        final AtomicReference<String> pastTimeSlotId = new AtomicReference<>();
        final AtomicReference<LocalDate> pastTimeSlotDate = new AtomicReference<>();

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

        @Override
        public void pastTimeSlot(String timeSlotId, LocalDate date)
        {
            pastTimeSlotId.set(timeSlotId);
            pastTimeSlotDate.set(date);
        }
    }
}
