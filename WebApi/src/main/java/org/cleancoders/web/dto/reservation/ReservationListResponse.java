package org.cleancoders.web.dto.reservation;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "预约列表响应")
public record ReservationListResponse(
        @Schema(description = "预约列表")
        List<ReservationItemResponse> reservations
) {
}
