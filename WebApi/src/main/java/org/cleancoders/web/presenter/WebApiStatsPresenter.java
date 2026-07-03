package org.cleancoders.web.presenter;

import jakarta.inject.Singleton;
import jakarta.ws.rs.core.Response;
import org.cleancoders.systemtask.usecase.*;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("date", date.toString());
        body.put("totalSeats", totalSeats);
        body.put("usedSeats", usedSeats);
        body.put("usageRate", usageRate);
        responseContext.set(Response.ok(body).build());
    }

    @Override
    public void presentTimeSlotStats(LocalDate date, List<GetTimeSlotStatsUseCase.TimeSlotStatItem> items)
    {
        List<Map<String, Object>> slotList = items.stream()
                .map(item -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("timeSlotId", item.timeSlotId());
                    m.put("label", item.label());
                    m.put("count", item.count());
                    return m;
                })
                .toList();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("date", date.toString());
        body.put("timeSlots", slotList);
        responseContext.set(Response.ok(body).build());
    }

    @Override
    public void presentPopularRooms(LocalDate date, List<GetPopularRoomsStatsUseCase.PopularRoomItem> items)
    {
        List<Map<String, Object>> roomList = items.stream()
                .map(item -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("roomId", item.roomId());
                    m.put("roomName", item.roomName());
                    m.put("reservationCount", item.reservationCount());
                    return m;
                })
                .toList();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("date", date.toString());
        body.put("rooms", roomList);
        responseContext.set(Response.ok(body).build());
    }

    @Override
    public void presentCheckInRate(LocalDate date, int totalReservations, int checkedIn, double checkInRate)
    {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("date", date.toString());
        body.put("totalReservations", totalReservations);
        body.put("checkedIn", checkedIn);
        body.put("checkInRate", checkInRate);
        responseContext.set(Response.ok(body).build());
    }

    @Override
    public void presentNoShowRate(LocalDate date, int totalReservations, int expired, double noShowRate)
    {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("date", date.toString());
        body.put("totalReservations", totalReservations);
        body.put("expired", expired);
        body.put("noShowRate", noShowRate);
        responseContext.set(Response.ok(body).build());
    }
}
