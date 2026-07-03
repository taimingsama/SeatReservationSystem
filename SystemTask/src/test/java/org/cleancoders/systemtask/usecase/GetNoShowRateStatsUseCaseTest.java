package org.cleancoders.systemtask.usecase;

import org.cleancoders.common.domain.User;
import org.cleancoders.common.domain.UserRole;
import org.cleancoders.common.usecase.AdminAuthUseCase;
import org.cleancoders.common.usecase.AuthUseCase;
import org.cleancoders.common_test_infrastructure.StubTokenService;
import org.cleancoders.common_test_infrastructure.StubUserRepo;
import org.cleancoders.reservation.domain.Reservation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class GetNoShowRateStatsUseCaseTest
{

    private GetNoShowRateStatsUseCase useCase;
    private GetSeatUsageStatsUseCaseTest.StubReservationRepo reservationRepo;
    private StubPresenter presenter;
    private StubTokenService tokenService;
    private StubUserRepo userRepo;

    @BeforeEach
    void setUp()
    {
        useCase = new GetNoShowRateStatsUseCase();
        reservationRepo = new GetSeatUsageStatsUseCaseTest.StubReservationRepo();
        presenter = new StubPresenter();
        tokenService = new StubTokenService();
        userRepo = new StubUserRepo();

        useCase.presenter = presenter;
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
    void shouldReturnCorrectNoShowRate()
    {
        LocalDate today = LocalDate.now();
        reservationRepo.save(new Reservation("r1", "u1", "s1", "ts-1", today));
        reservationRepo.save(new Reservation("r2", "u2", "s2", "ts-1", today));
        Reservation r3 = new Reservation("r3", "u3", "s3", "ts-2", today);
        reservationRepo.save(r3);
        r3.expire();

        GetNoShowRateStatsUseCase.Output output = useCase.execute(
                new GetNoShowRateStatsUseCase.Request("valid-token"));

        assertNotNull(output);
        assertEquals(3, output.totalReservations());
        assertEquals(1, output.expired());
        assertEquals(0.333, output.noShowRate());
    }

    @Test
    void shouldNotCountCancelledAsExpired()
    {
        LocalDate today = LocalDate.now();
        reservationRepo.save(new Reservation("r1", "u1", "s1", "ts-1", today));
        Reservation r2 = new Reservation("r2", "u2", "s2", "ts-1", today);
        reservationRepo.save(r2);
        r2.cancel();

        GetNoShowRateStatsUseCase.Output output = useCase.execute(
                new GetNoShowRateStatsUseCase.Request("valid-token"));

        assertNotNull(output);
        assertEquals(2, output.totalReservations());
        assertEquals(0, output.expired());
    }

    @Test
    void shouldReturnZeroWhenNoTodayReservations()
    {
        GetNoShowRateStatsUseCase.Output output = useCase.execute(
                new GetNoShowRateStatsUseCase.Request("valid-token"));

        assertNotNull(output);
        assertEquals(0, output.totalReservations());
        assertEquals(0, output.expired());
        assertEquals(0.0, output.noShowRate());
    }

    @Test
    void shouldReturn403WhenNotAdmin()
    {
        User student = new User("student-1", "alice", "pass", UserRole.STUDENT, "Alice", "a@b.com");
        userRepo.addUser(student);
        tokenService.setUserId(student.id());

        GetNoShowRateStatsUseCase.Output output = useCase.execute(
                new GetNoShowRateStatsUseCase.Request("valid-token"));

        assertNull(output);
        assertTrue(presenter.forbiddenCalled);
    }

    static class StubPresenter implements GetNoShowRateStatsUseCase.Presenter,
            AdminAuthUseCase.Presenter, AuthUseCase.Presenter
    {
        boolean presentNoShowRateCalled;
        LocalDate date;
        int totalReservations, expired;
        double noShowRate;
        boolean forbiddenCalled, invalidTokenCalled, userNotFoundCalled;

        @Override
        public void presentNoShowRate(LocalDate date, int totalReservations, int expired, double noShowRate)
        {
            this.presentNoShowRateCalled = true;
            this.date = date;
            this.totalReservations = totalReservations;
            this.expired = expired;
            this.noShowRate = noShowRate;
        }

        @Override public void forbidden() { forbiddenCalled = true; }
        @Override public void invalidToken() { invalidTokenCalled = true; }
        @Override public void userNotFound() { userNotFoundCalled = true; }
    }
}
