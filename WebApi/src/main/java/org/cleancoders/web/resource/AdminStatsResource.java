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
import org.cleancoders.systemtask.usecase.*;
import org.cleancoders.web.dto.common.ErrorResponse;
import org.cleancoders.web.dto.stats.*;
import org.cleancoders.web.presenter.ResponseContext;

@Path("/admin/stats")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Admin Stats", description = "管理员数据统计接口")
public class AdminStatsResource
{

    @Inject
    GetSeatUsageStatsUseCase getSeatUsageStatsUseCase;

    @Inject
    GetTimeSlotStatsUseCase getTimeSlotStatsUseCase;

    @Inject
    GetPopularRoomsStatsUseCase getPopularRoomsStatsUseCase;

    @Inject
    GetCheckInRateStatsUseCase getCheckInRateStatsUseCase;

    @Inject
    GetNoShowRateStatsUseCase getNoShowRateStatsUseCase;

    @Inject
    ResponseContext responseContext;

    @GET
    @Path("/seat-usage")
    @Operation(summary = "今日座位使用率 (UC-15)", description = "统计今日被占用座位数占总可用座位数的比例。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = SeatUsageStatsResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token 无效或已过期",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "权限不足（非管理员角色）",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Response seatUsage(
            @Parameter(description = "JWT 认证 token", required = true)
            @CookieParam("Authorization") String authCookie)
    {
        getSeatUsageStatsUseCase.execute(new GetSeatUsageStatsUseCase.Request(authCookie));
        return responseContext.get();
    }

    @GET
    @Path("/time-slot")
    @Operation(summary = "今日各时段预约量 (UC-15)", description = "按固定时段统计今日预约数量。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = TimeSlotStatsResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token 无效或已过期",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "权限不足（非管理员角色）",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Response timeSlotStats(
            @Parameter(description = "JWT 认证 token", required = true)
            @CookieParam("Authorization") String authCookie)
    {
        getTimeSlotStatsUseCase.execute(new GetTimeSlotStatsUseCase.Request(authCookie));
        return responseContext.get();
    }

    @GET
    @Path("/popular-rooms")
    @Operation(summary = "今日热门自习室排名 (UC-15)", description = "按今日预约量对自习室降序排名。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = PopularRoomsStatsResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token 无效或已过期",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "权限不足（非管理员角色）",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Response popularRooms(
            @Parameter(description = "JWT 认证 token", required = true)
            @CookieParam("Authorization") String authCookie)
    {
        getPopularRoomsStatsUseCase.execute(new GetPopularRoomsStatsUseCase.Request(authCookie));
        return responseContext.get();
    }

    @GET
    @Path("/check-in-rate")
    @Operation(summary = "今日签到率 (UC-15)", description = "统计今日已签到预约数占总预约数的比例。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = CheckInRateStatsResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token 无效或已过期",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "权限不足（非管理员角色）",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Response checkInRate(
            @Parameter(description = "JWT 认证 token", required = true)
            @CookieParam("Authorization") String authCookie)
    {
        getCheckInRateStatsUseCase.execute(new GetCheckInRateStatsUseCase.Request(authCookie));
        return responseContext.get();
    }

    @GET
    @Path("/no-show-rate")
    @Operation(summary = "今日违约率 (UC-15)", description = "统计今日超时未签到预约数占总预约数的比例。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = NoShowRateStatsResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token 无效或已过期",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "权限不足（非管理员角色）",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Response noShowRate(
            @Parameter(description = "JWT 认证 token", required = true)
            @CookieParam("Authorization") String authCookie)
    {
        getNoShowRateStatsUseCase.execute(new GetNoShowRateStatsUseCase.Request(authCookie));
        return responseContext.get();
    }
}
