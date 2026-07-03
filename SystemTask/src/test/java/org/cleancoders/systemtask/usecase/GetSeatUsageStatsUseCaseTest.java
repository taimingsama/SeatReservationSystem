package org.cleancoders.systemtask.usecase;

import org.cleancoders.reservation.domain.Reservation;
import org.cleancoders.reservation.domain.ReservationStatus;
import org.cleancoders.reservation.outbound.ReservationRepository;
import org.cleancoders.seatandroom.domain.Seat;
import org.cleancoders.seatandroom.domain.SeatStatus;
import org.cleancoders.seatandroom_test_infrastructure.StubSeatRepo;
import org.cleancoders.userandauth.domain.User;
import org.cleancoders.userandauth.domain.UserRole;
import org.cleancoders.userandauth.usecase.AdminAuthUseCase;
import org.cleancoders.userandauth.usecase.AuthUseCase;
import org.cleancoders.userandauth_test_infrastructure.StubTokenService;
import org.cleancoders.userandauth_test_infrastructure.StubUserRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class GetSeatUsageStatsUseCaseTest
{

    private GetSeatUsageStatsUseCase useCase;
    private StubSeatRepo seatRepo;
    private StubReservationRepo reservationRepo;
    private StubPresenter presenter;
    private StubTokenService tokenService;
    private StubUserRepo userRepo;

    @BeforeEach
    void setUp()
    {
        useCase = new GetSeatUsageStatsUseCase();
        seatRepo = new StubSeatRepo();
        reservationRepo = new StubReservationRepo();
        presenter = new StubPresenter();
        tokenService = new StubTokenService();
        userRepo = new StubUserRepo();

        useCase.presenter = presenter;
        useCase.seatRepo = seatRepo;
        useCase.reservationRepo = reservationRepo;
        useCase.tokenService = tokenService;
        useCase.userRepo = userRepo;
        ((AdminAuthUseCase<?, ?>) useCase).presenter = presenter;
        ((AuthUseCase<?, ?>) useCase).presenter = presenter;

        User admin = new User("admin-1", "admin", "pass", UserRole.ADMIN, "Admin", "admin@test.com");
        userRepo.addUser(admin);
        tokenService.setUserId(admin.id());
    }

    @Test
    void shouldReturnCorrectUsageRate()
    {
        seatRepo.addSeat(new Seat(1, "room-1", SeatStatus.AVAILABLE));
        seatRepo.addSeat(new Seat(2, "room-1", SeatStatus.AVAILABLE));
        seatRepo.addSeat(new Seat(3, "room-1", SeatStatus.MAINTENANCE));
        seatRepo.addSeat(new Seat(4, "room-1", SeatStatus.REMOVED));

        LocalDate today = LocalDate.now();
        Reservation r1 = new Reservation("r1", "user-1", "room-1", 1, "ts-1", today);
        reservationRepo.save(r1);
        Reservation r2 = new Reservation("r2", "user-2", "room-1", 3, "ts-2", today);
        reservationRepo.save(r2);
        r2.checkIn();

        GetSeatUsageStatsUseCase.Output output = useCase.execute(
                new GetSeatUsageStatsUseCase.Request("valid-token"));

        assertNotNull(output);
        assertEquals(3, output.totalSeats());
        assertEquals(2, output.usedSeats());
        assertEquals(0.667, output.usageRate());

        assertTrue(presenter.presentSeatUsageCalled);
        assertEquals(today, presenter.date);
        assertEquals(3, presenter.totalSeats);
        assertEquals(2, presenter.usedSeats);
        assertEquals(0.667, presenter.usageRate);
    }

    @Test
    void shouldReturnZeroWhenNoSeats()
    {
        GetSeatUsageStatsUseCase.Output output = useCase.execute(
                new GetSeatUsageStatsUseCase.Request("valid-token"));

        assertNotNull(output);
        assertEquals(0, output.totalSeats());
        assertEquals(0, output.usedSeats());
        assertEquals(0.0, output.usageRate());
    }

    @Test
    void shouldReturnZeroWhenNoTodayReservations()
    {
        seatRepo.addSeat(new Seat(1, "room-1", SeatStatus.AVAILABLE));
        seatRepo.addSeat(new Seat(2, "room-1", SeatStatus.AVAILABLE));

        Reservation old = new Reservation("r-old", "user-1", "room-1", 1, "ts-1",
                LocalDate.now().minusDays(1));
        reservationRepo.save(old);

        GetSeatUsageStatsUseCase.Output output = useCase.execute(
                new GetSeatUsageStatsUseCase.Request("valid-token"));

        assertNotNull(output);
        assertEquals(2, output.totalSeats());
        assertEquals(0, output.usedSeats());
        assertEquals(0.0, output.usageRate());
    }

    @Test
    void shouldDeduplicateSeats()
    {
        seatRepo.addSeat(new Seat(1, "room-1", SeatStatus.AVAILABLE));

        LocalDate today = LocalDate.now();
        reservationRepo.save(new Reservation("r1", "user-1", "room-1", 1, "ts-1", today));
        reservationRepo.save(new Reservation("r2", "user-2", "room-1", 1, "ts-2", today));

        GetSeatUsageStatsUseCase.Output output = useCase.execute(
                new GetSeatUsageStatsUseCase.Request("valid-token"));

        assertNotNull(output);
        assertEquals(1, output.totalSeats());
        assertEquals(1, output.usedSeats());
    }

    @Test
    void shouldReturn403WhenNotAdmin()
    {
        User student = new User("student-1", "alice", "pass", UserRole.STUDENT, "Alice", "a@b.com");
        userRepo.addUser(student);
        tokenService.setUserId(student.id());

        GetSeatUsageStatsUseCase.Output output = useCase.execute(
                new GetSeatUsageStatsUseCase.Request("valid-token"));

        assertNull(output);
        assertTrue(presenter.forbiddenCalled);
    }

    // --- Shared Stubs ---

    static class StubReservationRepo implements ReservationRepository
    {
        private final Map<String, Reservation> store = new LinkedHashMap<>();

        

        @Override public Reservation save(Reservation r) { if (r.id() == null) { throw new IllegalArgumentException("id required"); } store.put(r.id(), r); return r; }
        @Override public Optional<Reservation> findById(String id) { return Optional.ofNullable(store.get(id)); }
        @Override public Optional<Reservation> findByUserIdAndDateAndTimeSlotIdAndStatusIn(
                String uid, LocalDate d, String ts, Set<ReservationStatus> ss) { return Optional.empty(); }
        @Override public Optional<Reservation> findBySeatIdAndDateAndTimeSlotIdAndStatusIn(
                String roomId, int seatId, LocalDate d, String ts, Set<ReservationStatus> ss) { return Optional.empty(); }
        @Override public List<Reservation> findByUserId(String uid) { return List.of(); }
        @Override public List<Reservation> findAll() { return List.copyOf(store.values()); }
        @Override public List<Reservation> findBySeatIdAndStatusIn(String roomId, int seatId, Set<ReservationStatus> ss) { return List.of(); }
    }

    static class StubPresenter implements GetSeatUsageStatsUseCase.Presenter,
            AdminAuthUseCase.Presenter, AuthUseCase.Presenter
    {
        boolean presentSeatUsageCalled;
        LocalDate date;
        int totalSeats, usedSeats;
        double usageRate;
        boolean forbiddenCalled, invalidTokenCalled, userNotFoundCalled;

        @Override
        public void presentSeatUsage(LocalDate date, int totalSeats, int usedSeats, double usageRate)
        {
            this.presentSeatUsageCalled = true;
            this.date = date;
            this.totalSeats = totalSeats;
            this.usedSeats = usedSeats;
            this.usageRate = usageRate;
        }

        @Override public void forbidden() { forbiddenCalled = true; }
        @Override public void invalidToken() { invalidTokenCalled = true; }
        @Override public void userNotFound() { userNotFoundCalled = true; }
    }
}
