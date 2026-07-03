package org.cleancoders.web.resource;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.cleancoders.seatandroom.usecase.ListRoomsUseCase;
import org.cleancoders.seatandroom.usecase.ListSeatsUseCase;
import org.cleancoders.seatandroom.usecase.ListTimeSlotsUseCase;
import org.cleancoders.web.dto.room.RoomListResponse;
import org.cleancoders.web.dto.room.RoomNotFoundResponse;
import org.cleancoders.web.dto.seat.SeatListResponse;
import org.cleancoders.web.dto.seat.TimeSlotListResponse;
import org.cleancoders.web.presenter.ResponseContext;

import java.time.LocalDate;

@Path("/rooms")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Room", description = "自习室相关接口")
public class RoomResource
{

    @Inject
    ListRoomsUseCase listRoomsUseCase;

    @Inject
    ListSeatsUseCase listSeatsUseCase;

    @Inject
    ListTimeSlotsUseCase listTimeSlotsUseCase;

    @Inject
    ResponseContext responseContext;

    @GET
    @Path("/timeslots")
    @Operation(summary = "获取所有时间段", description = "公开接口，返回系统预设的全部时间段。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "返回时间段列表",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = TimeSlotListResponse.class)))
    })
    public Response listTimeSlots()
    {
        listTimeSlotsUseCase.execute();
        return responseContext.get();
    }

    @GET
    @Operation(summary = "获取所有 OPEN 状态的自习室 (UC-04)", description = "公开接口,返回当前状态为 OPEN 的全部自习室。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "返回 OPEN 状态自习室列表(可为空)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = RoomListResponse.class)))
    })
    public Response listRooms()
    {
        listRoomsUseCase.execute(new ListRoomsUseCase.Request());
        return responseContext.get();
    }

    @GET
    @Path("/{id}/seats")
    @Operation(summary = "获取某自习室所有座位及状态 (UC-05)", description = "公开接口,返回指定自习室的全部座位及其当前状态。可选传入时段和日期,根据预约情况返回对应时段的实际可用状态。自习室不存在时返回 404。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "返回座位列表(可为空)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = SeatListResponse.class))),
            @ApiResponse(responseCode = "404", description = "自习室不存在",
                    content = @Content(schema = @Schema(implementation = RoomNotFoundResponse.class)))
    })
    public Response listSeats(
            @Parameter(description = "自习室ID", required = true, example = "room-1")
            @PathParam("id") String roomId,
            @Parameter(description = "时段ID（可选，传入后返回该时段的实际可用状态）", example = "ts-1")
            @QueryParam("timeSlotId") String timeSlotId,
            @Parameter(description = "日期（可选，与 timeSlotId 配合使用，格式 YYYY-MM-DD）", example = "2026-07-03")
            @QueryParam("date") String dateStr)
    {
        LocalDate date = null;
        if (dateStr != null && !dateStr.isBlank())
        {
            date = LocalDate.parse(dateStr);
        }
        listSeatsUseCase.execute(new ListSeatsUseCase.Request(roomId, timeSlotId, date));
        return responseContext.get();
    }
}