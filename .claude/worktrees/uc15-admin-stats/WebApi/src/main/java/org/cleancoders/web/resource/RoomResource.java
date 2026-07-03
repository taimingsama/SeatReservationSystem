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
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.cleancoders.seatandroom.usecase.ListRoomsUseCase;
import org.cleancoders.seatandroom.usecase.ListSeatsUseCase;
import org.cleancoders.web.dto.room.RoomListResponse;
import org.cleancoders.web.dto.seat.SeatListResponse;
import org.cleancoders.web.presenter.ResponseContext;

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
    ResponseContext responseContext;

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
    @Operation(summary = "获取某自习室所有座位及状态 (UC-05)", description = "公开接口,返回指定自习室的全部座位及其当前状态。自习室不存在时返回 404。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "返回座位列表(可为空)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = SeatListResponse.class))),
            @ApiResponse(responseCode = "404", description = "自习室不存在")
    })
    public Response listSeats(
            @Parameter(description = "自习室ID", required = true, example = "room-1")
            @PathParam("id") String roomId)
    {
        listSeatsUseCase.execute(new ListSeatsUseCase.Request(roomId));
        return responseContext.get();
    }
}