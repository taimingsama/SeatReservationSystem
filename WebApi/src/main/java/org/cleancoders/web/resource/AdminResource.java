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
import org.cleancoders.reservation.usecase.ManageReservationsUseCase;
import org.cleancoders.web.dto.common.ErrorResponse;
import org.cleancoders.web.presenter.WebApiReservationPresenter;

@Path("/admin")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Admin", description = "管理员相关接口")
public class AdminResource {

    @Inject
    ManageReservationsUseCase manageReservationsUseCase;

    @Inject
    WebApiReservationPresenter presenter;

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
        return presenter.getResponse();
    }
}
