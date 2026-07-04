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
        bind(MysqlSeatRepo.class).to(SeatRepository.class).in(Singleton.class);
        bind(MysqlTimeSlotRepo.class).to(TimeSlotRepository.class).in(Singleton.class);
        bind(MysqlRoomRepo.class).to(RoomRepository.class).in(Singleton.class);
        bind(MysqlReservationRepo.class).to(ReservationRepository.class).in(Singleton.class);
        bind(MysqlUserRepo.class).to(UserRepository.class).in(Singleton.class);
    }
}
