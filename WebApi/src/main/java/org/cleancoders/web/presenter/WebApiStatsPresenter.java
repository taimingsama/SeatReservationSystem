package org.cleancoders.web.presenter;

import jakarta.inject.Singleton;
import jakarta.ws.rs.core.Response;
import org.cleancoders.systemtask.usecase.*;
import org.cleancoders.web.dto.stats.*;

import java.time.LocalDate;
import java.util.List;

@Singleton
public class WebApiStatsPresenter extends WebApiPresenter implements
        GetSeatUsageStatsUseCase.Presenter,
        GetTimeSlotStatsUseCase.Presenter,
        GetPopularRoomsStatsUseCase.Presenter,
        GetCheckInRateStatsUseCase.Presenter,
        GetNoShowRateStatsUseCase.Presenter
{

    @Override
    public void presentSeatUsage(LocalDate date, int totalSeats, int usedSeats, double usageRate)
    {
        responseContext.set(Response.ok(
                new SeatUsageStatsResponse(date.toString(), totalSeats, usedSeats, usageRate)
        ).build());
    }

    @Override
    public void presentTimeSlotStats(LocalDate date, List<GetTimeSlotStatsUseCase.TimeSlotStatItem> items)
    {
        List<TimeSlotStatItemResponse> slotList = items.stream()
                .map(item -> new TimeSlotStatItemResponse(
                        item.timeSlotId(), item.label(), item.count()))
                .toList();

        responseContext.set(Response.ok(
                new TimeSlotStatsResponse(date.toString(), slotList)
        ).build());
    }

    @Override
    public void presentPopularRooms(LocalDate date, List<GetPopularRoomsStatsUseCase.PopularRoomItem> items)
    {
        List<PopularRoomItemResponse> roomList = items.stream()
                .map(item -> new PopularRoomItemResponse(
                        item.roomId(), item.roomName(), item.reservationCount()))
                .toList();

        responseContext.set(Response.ok(
                new PopularRoomsStatsResponse(date.toString(), roomList)
        ).build());
    }

    @Override
    public void presentCheckInRate(LocalDate date, int totalReservations, int checkedIn, double checkInRate)
    {
        responseContext.set(Response.ok(
                new CheckInRateStatsResponse(date.toString(), totalReservations, checkedIn, checkInRate)
        ).build());
    }

    @Override
    public void presentNoShowRate(LocalDate date, int totalReservations, int expired, double noShowRate)
    {
        responseContext.set(Response.ok(
                new NoShowRateStatsResponse(date.toString(), totalReservations, expired, noShowRate)
        ).build());
    }

}
