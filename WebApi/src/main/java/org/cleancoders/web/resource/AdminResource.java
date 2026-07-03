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
import org.cleancoders.seatandroom.usecase.UpdateSeatUseCase;
import org.cleancoders.web.dto.admin.CreateRoomRequest;
import org.cleancoders.web.dto.admin.UpdateSeatRequest;
import org.cleancoders.web.dto.common.ErrorResponse;
import org.cleancoders.web.dto.reservation.AdminReservationListResponse;
import org.cleancoders.web.dto.room.*;
import org.cleancoders.web.dto.seat.*;
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
    UpdateSeatUseCase updateSeatUseCase;

    @Inject
    ResponseContext responseContext;

    @GET
    @Path("/reservations")
    @Operation(summary = "查看所有预约 (UC-13)", description = "管理员查看系统中所有预约记录，包含用户、座位和时段信息。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功返回所有预约",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = AdminReservationListResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token 无效或已过期",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "权限不足（非管理员角色）",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Response listAllReservations(
            @Parameter(description = "JWT 认证 token", required = true)
            @CookieParam("Authorization") String authCookie) {
        manageReservationsUseCase.execute(new ManageReservationsUseCase.Request(authCookie));
        return responseContext.get();
    }

    @POST
    @Path("/rooms")
    @Operation(summary = "创建自习室 (UC-06)", description = "管理员创建一个新的自习室，根据所选布局自动生成座位。")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "创建成功",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = RoomResponse.class))),
            @ApiResponse(responseCode = "400", description = "无效的布局类型",
                    content = @Content(schema = @Schema(implementation = InvalidLayoutResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token 无效或已过期",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "权限不足（非管理员角色）",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "自习室名称已存在",
                    content = @Content(schema = @Schema(implementation = RoomNameConflictResponse.class)))
    })
    public Response createRoom(
            @Parameter(description = "JWT 认证 token", required = true)
            @CookieParam("Authorization") String authCookie,
            CreateRoomRequest input)
    {
        manageRoomsUseCase.execute(new ManageRoomsUseCase.Request(
                authCookie, input.name(), input.location(), input.layout()));
        return responseContext.get();
    }

    @PUT
    @Path("/rooms/{roomId}/seats/{seatId}")
    @Operation(summary = "更新座位状态 (UC-07)", description = "管理员切换座位状态(AVAILABLE↔MAINTENANCE)。通过教室ID+座位序号定位。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "更新成功",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = SeatResponse.class))),
            @ApiResponse(responseCode = "400", description = "非法座位状态",
                    content = @Content(schema = @Schema(implementation = InvalidSeatStatusResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token 无效或已过期",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "权限不足（非管理员角色）",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "座位不存在",
                    content = @Content(schema = @Schema(implementation = SeatNotFoundInRoomResponse.class))),
            @ApiResponse(responseCode = "409", description = "非法状态转换",
                    content = @Content(schema = @Schema(implementation = InvalidStatusTransitionResponse.class)))
    })
    public Response updateSeat(
            @Parameter(description = "JWT 认证 token", required = true)
            @CookieParam("Authorization") String authCookie,
            @Parameter(description = "教室ID", required = true, example = "room-1")
            @PathParam("roomId") String roomId,
            @Parameter(description = "座位序号", required = true, example = "5")
            @PathParam("seatId") int seatId,
            UpdateSeatRequest input)
    {
        updateSeatUseCase.execute(new UpdateSeatUseCase.Request(
                authCookie, roomId, seatId, input.status()));
        return responseContext.get();
    }

    @PUT
    @Path("/rooms/{id}")
    @Operation(summary = "更新自习室 (UC-06)", description = "管理员更新指定自习室的名称和位置。布局不可更改。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "更新成功",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = RoomResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token 无效或已过期",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "权限不足（非管理员角色）",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "自习室不存在",
                    content = @Content(schema = @Schema(implementation = RoomNotFoundResponse.class))),
            @ApiResponse(responseCode = "409", description = "自习室名称已存在",
                    content = @Content(schema = @Schema(implementation = RoomNameConflictResponse.class)))
    })
    public Response updateRoom(
            @Parameter(description = "JWT 认证 token", required = true)
            @CookieParam("Authorization") String authCookie,
            @Parameter(description = "自习室ID", required = true, example = "room-1")
            @PathParam("id") String roomId,
            CreateRoomRequest input)
    {
        updateRoomUseCase.execute(new UpdateRoomUseCase.Request(
                authCookie, roomId, input.name(), input.location()));
        return responseContext.get();
    }

    @DELETE
    @Path("/rooms/{id}")
    @Operation(summary = "删除自习室 (UC-06)", description = "管理员将自习室状态标记为 CLOSED（软删除），同时级联删除所有座位。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "删除成功",
                    content = @Content(schema = @Schema(implementation = RoomDeletedResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token 无效或已过期",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "权限不足（非管理员角色）",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "自习室不存在",
                    content = @Content(schema = @Schema(implementation = RoomNotFoundResponse.class))),
            @ApiResponse(responseCode = "409", description = "自习室已处于关闭状态",
                    content = @Content(schema = @Schema(implementation = RoomAlreadyClosedResponse.class)))
    })
    public Response deleteRoom(
            @Parameter(description = "JWT 认证 token", required = true)
            @CookieParam("Authorization") String authCookie,
            @Parameter(description = "自习室ID", required = true, example = "room-1")
            @PathParam("id") String roomId)
    {
        deleteRoomUseCase.execute(new DeleteRoomUseCase.Request(authCookie, roomId));
        return responseContext.get();
    }
}
