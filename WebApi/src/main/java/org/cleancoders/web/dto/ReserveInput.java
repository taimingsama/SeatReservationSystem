package org.cleancoders.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "预约创建请求")
public record ReserveInput(
        @Schema(description = "座位 ID", example = "seat-1")
        String seatId,

        @Schema(description = "时段 ID", example = "ts-1")
        String timeSlotId,

        @Schema(description = "预约日期 (ISO 格式: YYYY-MM-DD)", example = "2026-07-02")
        String date
) {
}
