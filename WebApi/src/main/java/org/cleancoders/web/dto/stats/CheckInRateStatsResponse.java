package org.cleancoders.web.dto.stats;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "签到率统计响应")
public record CheckInRateStatsResponse(
        @Schema(description = "统计日期", example = "2025-01-15")
        String date,
        @Schema(description = "总预约数", example = "100")
        int totalReservations,
        @Schema(description = "已签到数", example = "78")
        int checkedIn,
        @Schema(description = "签到率", example = "0.78")
        double checkInRate
) {
}
