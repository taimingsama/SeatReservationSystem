package org.cleancoders.web.binder;

import jakarta.inject.Singleton;
import org.cleancoders.infrastructure.persistence.mysql.*;
import org.cleancoders.reservation.outbound.ReservationRepository;
import org.cleancoders.seatandroom.outbound.RoomRepository;
import org.cleancoders.seatandroom.outbound.SeatRepository;
import org.cleancoders.seatandroom.outbound.TimeSlotRepository;
import org.cleancoders.userandauth.outbound.UserRepository;
import org.glassfish.hk2.utilities.binding.AbstractBinder;

public class MySqlBinder extends AbstractBinder
{
    @Override
    protected void configure()
    {
        bind(MysqlConnectionProvider.class).to(MysqlConnectionProvider.class).in(Singleton.class);
        // MySQL bindings use ranked(10) so they override TestData bindings (default rank 0)
        bind(MysqlSeatRepo.class).to(SeatRepository.class).in(Singleton.class).ranked(10);
        bind(MysqlTimeSlotRepo.class).to(TimeSlotRepository.class).in(Singleton.class).ranked(10);
        bind(MysqlRoomRepo.class).to(RoomRepository.class).in(Singleton.class).ranked(10);
        bind(MysqlReservationRepo.class).to(ReservationRepository.class).in(Singleton.class).ranked(10);
        bind(MysqlUserRepo.class).to(UserRepository.class).in(Singleton.class).ranked(10);
    }
}
