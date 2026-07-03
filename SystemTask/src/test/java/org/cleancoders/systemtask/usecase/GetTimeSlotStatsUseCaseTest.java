package org.cleancoders.systemtask.usecase;

import org.cleancoders.reservation.domain.Reservation;
import org.cleancoders.seatandroom.domain.TimeSlot;
import org.cleancoders.seatandroom.outbound.TimeSlotRepository;
import org.cleancoders.userandauth.domain.User;
import org.cleancoders.userandauth.domain.UserRole;
import org.cleancoders.userandauth.usecase.AdminAuthUseCase;
import org.cleancoders.userandauth.usecase.AuthUseCase;
import org.cleancoders.userandauth_test_infrastructure.StubTokenService;
import org.cleancoders.userandauth_test_infrastructure.StubUserRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class GetTimeSlotStatsUseCaseTest
{

    private GetTimeSlotStatsUseCase useCase;
    private StubTimeSlotRepo timeSlotRepo;
    private GetSeatUsageStatsUseCaseTest.StubReservationRepo reservationRepo;
    private StubPresenter presenter;
    private StubTokenService tokenService;
    private StubUserRepo userRepo;

    @BeforeEach
    void setUp()
    {
        useCase = new GetTimeSlotStatsUseCase();
        timeSlotRepo = new StubTimeSlotRepo();
        reservationRepo = new GetSeatUsageStatsUseCaseTest.StubReservationRepo();
        presenter = new StubPresenter();
        tokenService = new StubTokenService();
        userRepo = new StubUserRepo();

        useCase.presenter = presenter;
        useCase.timeSlotRepo = timeSlotRepo;
        useCase.reservationRepo = reservationRepo;
        useCase.tokenService = tokenService;
        useCase.userRepo = userRepo;
        ((AdminAuthUseCase<?, ?>) useCase).presenter = presenter;
        ((AuthUseCase<?, ?>) useCase).presenter = presenter;

        User admin = new User("admin-1", "admin", "pass", UserRole.ADMIN, "Admin", "admin@test.com");
        userRepo.addUser(admin);
        tokenService.setUserId(admin.id());

        timeSlotRepo.addTimeSlot(new TimeSlot("ts-1", "08:00", "12:00", "08:00-12:00"));
        timeSlotRepo.addTimeSlot(new TimeSlot("ts-2", "13:00", "17:00", "13:00-17:00"));
        timeSlotRepo.addTimeSlot(new TimeSlot("ts-3", "18:00", "22:00", "18:00-22:00"));
    }

    @Test
    void shouldCountReservationsPerTimeSlot()
    {
        LocalDate today = LocalDate.now();
        reservationRepo.save(new Reservation("r1", "u1", "s1", "ts-1", today));
        reservationRepo.save(new Reservation("r2", "u2", "s2", "ts-1", today));
        reservationRepo.save(new Reservation("r3", "u3", "s3", "ts-2", today));

        GetTimeSlotStatsUseCase.Output output = useCase.execute(
                new GetTimeSlotStatsUseCase.Request("valid-token"));

        assertNotNull(output);
        assertEquals(3, output.items().size());
        assertEquals(2, output.items().get(0).count());
        assertEquals(1, output.items().get(1).count());
        assertEquals(0, output.items().get(2).count());
    }

    @Test
    void shouldReturnZeroWhenNoTodayReservations()
    {
        reservationRepo.save(new Reservation("r-old", "u1", "s1", "ts-1",
                LocalDate.now().minusDays(1)));

        GetTimeSlotStatsUseCase.Output output = useCase.execute(
                new GetTimeSlotStatsUseCase.Request("valid-token"));

        assertNotNull(output);
        assertTrue(output.items().stream().allMatch(item -> item.count() == 0));
    }

    @Test
    void shouldReturn403WhenNotAdmin()
    {
        User student = new User("student-1", "alice", "pass", UserRole.STUDENT, "Alice", "a@b.com");
        userRepo.addUser(student);
        tokenService.setUserId(student.id());

        GetTimeSlotStatsUseCase.Output output = useCase.execute(
                new GetTimeSlotStatsUseCase.Request("valid-token"));

        assertNull(output);
        assertTrue(presenter.forbiddenCalled);
    }

    // --- Stubs ---

    static class StubTimeSlotRepo implements TimeSlotRepository
    {
        private final Map<String, TimeSlot> store = new LinkedHashMap<>();
        void addTimeSlot(TimeSlot ts) { store.put(ts.id(), ts); }
        @Override public Optional<TimeSlot> findById(String id) { return Optional.ofNullable(store.get(id)); }
        @Override public List<TimeSlot> findAll() { return List.copyOf(store.values()); }
    }

    static class StubPresenter implements GetTimeSlotStatsUseCase.Presenter,
            AdminAuthUseCase.Presenter, AuthUseCase.Presenter
    {
        boolean presentTimeSlotStatsCalled;
        LocalDate date;
        List<GetTimeSlotStatsUseCase.TimeSlotStatItem> items;
        boolean forbiddenCalled, invalidTokenCalled, userNotFoundCalled;

        @Override
        public void presentTimeSlotStats(LocalDate date, List<GetTimeSlotStatsUseCase.TimeSlotStatItem> items)
        {
            this.presentTimeSlotStatsCalled = true;
            this.date = date;
            this.items = items;
        }

        @Override public void forbidden() { forbiddenCalled = true; }
        @Override public void invalidToken() { invalidTokenCalled = true; }
        @Override public void userNotFound() { userNotFoundCalled = true; }
    }
}
