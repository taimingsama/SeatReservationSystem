package org.cleancoders.web.resource;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.cleancoders.reservation.usecase.ManageReservationsUseCase;
import org.cleancoders.seatandroom.usecase.DeleteRoomUseCase;
import org.cleancoders.seatandroom.usecase.ManageRoomsUseCase;
import org.cleancoders.seatandroom.usecase.UpdateRoomUseCase;
import org.cleancoders.web.dto.admin.CreateRoomRequest;
import org.cleancoders.web.dto.common.ErrorResponse;
import org.cleancoders.web.dto.room.RoomResponse;
import org.cleancoders.web.presenter.ResponseContext;

@Path("/admin")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Admin", description = "管理员接口")
public class AdminResource
{

    @Inject
    ManageRoomsUseCase manageRoomsUseCase;

    @Inject
    UpdateRoomUseCase updateRoomUseCase;

    @Inject
    DeleteRoomUseCase deleteRoomUseCase;

    @Inject
    ManageReservationsUseCase manageReservationsUseCase;

    @Inject
    ResponseContext responseContext;

    @GET
    @Path("/reservations")
    @Operation(summary = "查看所有预约 (UC-13)", description = "管理员查看系统中所有预约记录，包含用户、座位和时段信息。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功返回所有预约",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON)),
            @ApiResponse(responseCode = "401", description = "Token 无效或已过期",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "权限不足（非管理员角色）",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Response listAllReservations(@CookieParam("Authorization") String authCookie) {
        manageReservationsUseCase.execute(new ManageReservationsUseCase.Request(authCookie));
        return responseContext.get();
    }

    @POST
    @Path("/rooms")
    @Operation(summary = "创建自习室 (UC-06)", description = "管理员创建一个新的自习室，状态默认为 OPEN。")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "创建成功",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = RoomResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token 无效或已过期",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "权限不足（非管理员角色）",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "自习室名称已存在")
    })
    public Response createRoom(
            @CookieParam("Authorization") String authCookie,
            CreateRoomRequest input)
    {
        manageRoomsUseCase.execute(new ManageRoomsUseCase.Request(
                authCookie, input.name(), input.location(), input.capacity()));
        return responseContext.get();
    }

    @PUT
    @Path("/rooms/{id}")
    @Operation(summary = "更新自习室 (UC-06)", description = "管理员更新指定自习室的名称、位置和容量。自习室不存在时返回 404。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "更新成功",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = RoomResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token 无效或已过期",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "权限不足（非管理员角色）",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "自习室不存在"),
            @ApiResponse(responseCode = "409", description = "自习室名称已存在")
    })
    public Response updateRoom(
            @CookieParam("Authorization") String authCookie,
            @Parameter(description = "自习室ID", required = true, example = "room-1")
            @PathParam("id") String roomId,
            CreateRoomRequest input)
    {
        updateRoomUseCase.execute(new UpdateRoomUseCase.Request(
                authCookie, roomId, input.name(), input.location(), input.capacity()));
        return responseContext.get();
    }

    @DELETE
    @Path("/rooms/{id}")
    @Operation(summary = "删除自习室 (UC-06)", description = "管理员将自习室状态标记为 CLOSED（软删除）。已处于 CLOSED 状态时返回 409。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "删除成功"),
            @ApiResponse(responseCode = "401", description = "Token 无效或已过期",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "权限不足（非管理员角色）",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "自习室不存在"),
            @ApiResponse(responseCode = "409", description = "自习室已处于关闭状态")
    })
    public Response deleteRoom(
            @CookieParam("Authorization") String authCookie,
            @Parameter(description = "自习室ID", required = true, example = "room-1")
            @PathParam("id") String roomId)
    {
        deleteRoomUseCase.execute(new DeleteRoomUseCase.Request(authCookie, roomId));
        return responseContext.get();
    }
}