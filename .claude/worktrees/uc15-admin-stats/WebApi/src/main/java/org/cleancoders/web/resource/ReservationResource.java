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
import org.cleancoders.reservation.usecase.CancelReservationUseCase;
import org.cleancoders.reservation.usecase.CheckInUseCase;
import org.cleancoders.reservation.usecase.CheckOutUseCase;
import org.cleancoders.reservation.usecase.ListMyReservationsUseCase;
import org.cleancoders.reservation.usecase.ReserveUseCase;
import org.cleancoders.web.dto.common.ErrorResponse;
import org.cleancoders.web.dto.reservation.InvalidDateResponse;
import org.cleancoders.web.dto.reservation.InvalidStatusResponse;
import org.cleancoders.web.dto.reservation.ReservationCreatedResponse;
import org.cleancoders.web.dto.reservation.ReservationNotFoundResponse;
import org.cleancoders.web.dto.reservation.ReserveInput;
import org.cleancoders.web.dto.reservation.SeatConflictResponse;
import org.cleancoders.web.dto.reservation.SeatNotFoundResponse;
import org.cleancoders.web.presenter.ResponseContext;

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
    CheckOutUseCase checkOutUseCase;

    @Inject
    CancelReservationUseCase cancelReservationUseCase;

    @Inject
    ListMyReservationsUseCase listMyReservationsUseCase;

    @Inject
    ResponseContext responseContext;

    @POST
    @Operation(summary = "创建预约 (UC-08)", description = "学生选择座位、时段和日期，通过冲突检测后创建预约。")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "预约创建成功",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = ReservationCreatedResponse.class))),
            @ApiResponse(responseCode = "400", description = "请求参数不合法（日期格式错误等）",
                    content = @Content(schema = @Schema(implementation = InvalidDateResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token 无效或已过期",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "权限不足（非学生角色）",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "座位或时段不存在",
                    content = @Content(schema = @Schema(implementation = SeatNotFoundResponse.class))),
            @ApiResponse(responseCode = "409", description = "座位已被预约",
                    content = @Content(schema = @Schema(implementation = SeatConflictResponse.class)))
    })
    public Response reserve(@CookieParam("Authorization") String authCookie, ReserveInput input) {
        LocalDate date;
        try {
            date = LocalDate.parse(input.date());
        } catch (Exception e) {
            return Response.status(400).entity(new InvalidDateResponse(
                    "日期格式不合法，请使用 YYYY-MM-DD 格式", input.date())).build();
        }

        reserveUseCase.execute(new ReserveUseCase.Request(
                authCookie, input.seatId(), input.timeSlotId(), date));
        return responseContext.get();
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
        return responseContext.get();
    }

    @POST
    @Path("/{id}/check-out")
    @Operation(summary = "退座 (UC-10)", description = "学生对已签到的座位进行退座操作，释放座位供他人使用。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "退座成功",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = ReservationCreatedResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token 无效或已过期",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "权限不足（非学生角色 / 非本人预约）",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "预约不存在",
                    content = @Content(schema = @Schema(implementation = ReservationNotFoundResponse.class))),
            @ApiResponse(responseCode = "409", description = "当前状态不允许退座",
                    content = @Content(schema = @Schema(implementation = InvalidStatusResponse.class)))
    })
    public Response checkOut(@CookieParam("Authorization") String authCookie, @PathParam("id") String reservationId) {
        checkOutUseCase.execute(new CheckOutUseCase.Request(authCookie, reservationId));
        return responseContext.get();
    }

    @DELETE
    @Path("/{id}")
    @Operation(summary = "取消预约 (UC-11)", description = "学生对未签到的预约进行取消操作。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "取消成功",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = ReservationCreatedResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token 无效或已过期",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "权限不足（非学生角色 / 非本人预约）",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "预约不存在",
                    content = @Content(schema = @Schema(implementation = ReservationNotFoundResponse.class))),
            @ApiResponse(responseCode = "409", description = "当前状态不允许取消",
                    content = @Content(schema = @Schema(implementation = InvalidStatusResponse.class)))
    })
    public Response cancel(@CookieParam("Authorization") String authCookie, @PathParam("id") String reservationId) {
        cancelReservationUseCase.execute(new CancelReservationUseCase.Request(authCookie, reservationId));
        return responseContext.get();
    }

    @GET
    @Path("/my")
    @Operation(summary = "我的预约 (UC-12)", description = "学生查看自己的所有预约记录（当前+历史），包含座位和时段信息。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功返回预约列表",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON)),
            @ApiResponse(responseCode = "401", description = "Token 无效或已过期",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "权限不足（非学生角色）",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Response myReservations(@CookieParam("Authorization") String authCookie) {
        listMyReservationsUseCase.execute(new ListMyReservationsUseCase.Request(authCookie));
        return responseContext.get();
    }
}
