package org.cleancoders.web.dto.reservation;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "座位冲突响应")
public record SeatConflictResponse(
        @Schema(description = "错误信息", example = "座位已被预约")
        String error,
        @Schema(description = "座位 ID", example = "seat-1")
        String seatId,
        @Schema(description = "时段标签", example = "上午 08:00-12:00")
        String timeSlot
) {
}
