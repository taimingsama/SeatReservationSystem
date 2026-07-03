package org.cleancoders.web.dto.reservation;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "管理端预约列表响应")
public record AdminReservationListResponse(
        @Schema(description = "预约列表")
        List<AdminReservationItemResponse> reservations
) {
}
