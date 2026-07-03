package org.cleancoders.web.dto.reservation;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "预约创建请求")
public record ReserveInput(
        @Schema(description = "教室 ID", example = "room-1")
        String roomId,

        @Schema(description = "座位序号 (1-N)", example = "5")
        int seatId,

        @Schema(description = "时段 ID", example = "ts-1")
        String timeSlotId,

        @Schema(description = "预约日期 (ISO 格式: YYYY-MM-DD)", example = "2026-07-02")
        String date
) {
}
