package org.cleancoders.web.resource;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.cleancoders.reservation.usecase.CheckInUseCase;
import org.cleancoders.reservation.usecase.ReserveUseCase;
import org.cleancoders.web.dto.*;
import org.cleancoders.web.presenter.WebApiReservationPresenter;

import java.time.LocalDate;

@Path("/reservations")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Reservation", description = "预约相关接口")
public class ReservationResource {

    @Inject
    ReserveUseCase reserveUseCase;

    @Inject
    CheckInUseCase checkInUseCase;

    @Inject
    WebApiReservationPresenter presenter;

    @POST
    @Operation(summary = "创建预约 (UC-08)", description = "学生选择座位、时段和日期，通过冲突检测后创建预约。")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "预约创建成功",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = ReservationCreatedResponse.class))),
            @ApiResponse(responseCode = "400", description = "请求参数不合法",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token 无效或已过期",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "权限不足（非学生角色）",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "座位或时段不存在",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "座位已被预约",
                    content = @Content(schema = @Schema(implementation = SeatConflictResponse.class)))
    })
    public Response reserve(@CookieParam("Authorization") String authCookie, ReserveInput input) {
        LocalDate date;
        try {
            date = LocalDate.parse(input.date());
        } catch (Exception e) {
            return Response.status(400).entity(java.util.Map.of(
                    "error", "日期格式不合法，请使用 YYYY-MM-DD 格式",
                    "provided", input.date()
            )).build();
        }

        reserveUseCase.execute(new ReserveUseCase.Request(
                authCookie, input.seatId(), input.timeSlotId(), date));
        return presenter.getResponse();
    }

    @POST
    @Path("/{id}/check-in")
    @Operation(summary = "签到 (UC-09)", description = "学生对已预约的座位进行签到，需在签到时间窗口内完成。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "签到成功",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = ReservationCreatedResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token 无效或已过期",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "权限不足（非学生角色 / 非本人预约）",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "预约不存在",
                    content = @Content(schema = @Schema(implementation = ReservationNotFoundResponse.class))),
            @ApiResponse(responseCode = "409", description = "当前状态不允许签到或不在签到时间窗口内",
                    content = @Content(schema = @Schema(implementation = InvalidStatusResponse.class)))
    })
    public Response checkIn(@CookieParam("Authorization") String authCookie, @PathParam("id") String reservationId) {
        checkInUseCase.execute(new CheckInUseCase.Request(authCookie, reservationId));
        return presenter.getResponse();
    }
}
