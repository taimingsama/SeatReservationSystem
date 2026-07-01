package org.cleancoders.web.resource;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.cleancoders.reservation.usecase.ReserveUseCase;
import org.cleancoders.web.dto.ReserveInput;
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
    WebApiReservationPresenter presenter;

    @POST
    @Operation(summary = "创建预约", description = "学生选择座位、时段和日期，通过冲突检测后创建预约。")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "预约创建成功"),
            @ApiResponse(responseCode = "401", description = "Token 无效或已过期"),
            @ApiResponse(responseCode = "403", description = "权限不足（非学生角色）"),
            @ApiResponse(responseCode = "404", description = "座位或时段不存在"),
            @ApiResponse(responseCode = "409", description = "座位已被预约 / 该时段已有预约"),
            @ApiResponse(responseCode = "400", description = "请求参数不合法")
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
}
