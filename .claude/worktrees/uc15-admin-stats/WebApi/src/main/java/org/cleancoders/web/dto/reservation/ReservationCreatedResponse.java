package org.cleancoders.web.dto.reservation;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "预约创建/签到成功响应")
public record ReservationCreatedResponse(
        @Schema(description = "预约 ID", example = "res-1")
        String reservationId,
        @Schema(description = "座位编号", example = "A-1")
        String seatNumber,
        @Schema(description = "时段标签", example = "上午 08:00-12:00")
        String timeSlot
) {
}
