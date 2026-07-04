package org.cleancoders.systemtask.usecase;

import org.cleancoders.reservation.domain.Reservation;
import org.cleancoders.userandauth.domain.User;
import org.cleancoders.userandauth.domain.UserRole;
import org.cleancoders.userandauth.usecase.AdminAuthUseCase;
import org.cleancoders.userandauth.usecase.AuthUseCase;
import org.cleancoders.userandauth_test_infrastructure.StubTokenService;
import org.cleancoders.userandauth_test_infrastructure.StubUserRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class GetCheckInRateStatsUseCaseTest
{

    private GetCheckInRateStatsUseCase useCase;
    private GetSeatUsageStatsUseCaseTest.StubReservationRepo reservationRepo;
    private StubPresenter presenter;
    private StubTokenService tokenService;
    private StubUserRepo userRepo;

    @BeforeEach
    void setUp()
    {
        useCase = new GetCheckInRateStatsUseCase();
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
    void shouldReturnCorrectCheckInRate()
    {
        LocalDate today = LocalDate.now();
        reservationRepo.save(new Reservation("r1", "u1", "room-1", 1, "ts-1", today));
        reservationRepo.save(new Reservation("r2", "u2", "room-1", 1, "ts-1", today));
        Reservation r3 = new Reservation("r3", "u3", "room-1", 1, "ts-1", today);
        reservationRepo.save(r3);
        r3.checkIn();
        Reservation r4 = new Reservation("r4", "u4", "room-1", 1, "ts-2", today);
        reservationRepo.save(r4);
        r4.checkIn();

        GetCheckInRateStatsUseCase.Output output = useCase.execute(
                new GetCheckInRateStatsUseCase.Request("valid-token"));

        assertNotNull(output);
        assertEquals(4, output.totalReservations());
        assertEquals(2, output.checkedIn());
        assertEquals(0.5, output.checkInRate());
    }

    @Test
    void shouldCountCheckedOutAsCheckedIn()
    {
        LocalDate today = LocalDate.now();
        Reservation r = new Reservation("r1", "u1", "room-1", 1, "ts-1", today);
        reservationRepo.save(r);
        r.checkIn();
        r.checkOut();

        GetCheckInRateStatsUseCase.Output output = useCase.execute(
                new GetCheckInRateStatsUseCase.Request("valid-token"));

        assertNotNull(output);
        assertEquals(1, output.totalReservations());
        assertEquals(1, output.checkedIn());
    }

    @Test
    void shouldReturnZeroWhenNoTodayReservations()
    {
        GetCheckInRateStatsUseCase.Output output = useCase.execute(
                new GetCheckInRateStatsUseCase.Request("valid-token"));

        assertNotNull(output);
        assertEquals(0, output.totalReservations());
        assertEquals(0, output.checkedIn());
        assertEquals(0.0, output.checkInRate());
    }

    @Test
    void shouldReturn403WhenNotAdmin()
    {
        User student = new User("student-1", "alice", "pass", UserRole.STUDENT, "Alice", "a@b.com");
        userRepo.addUser(student);
        tokenService.setUserId(student.id());

        GetCheckInRateStatsUseCase.Output output = useCase.execute(
                new GetCheckInRateStatsUseCase.Request("valid-token"));

        assertNull(output);
        assertTrue(presenter.forbiddenCalled);
    }

    static class StubPresenter implements GetCheckInRateStatsUseCase.Presenter,
            AdminAuthUseCase.Presenter, AuthUseCase.Presenter
    {
        boolean presentCheckInRateCalled;
        LocalDate date;
        int totalReservations, checkedIn;
        double checkInRate;
        boolean forbiddenCalled, invalidTokenCalled, userNotFoundCalled;

        @Override
        public void presentCheckInRate(LocalDate date, int totalReservations, int checkedIn, double checkInRate)
        {
            this.presentCheckInRateCalled = true;
            this.date = date;
            this.totalReservations = totalReservations;
            this.checkedIn = checkedIn;
            this.checkInRate = checkInRate;
        }

        @Override public void forbidden() { forbiddenCalled = true; }
        @Override public void invalidToken() { invalidTokenCalled = true; }
        @Override public void userNotFound() { userNotFoundCalled = true; }
    @Override public void banned() {}
    }
}
