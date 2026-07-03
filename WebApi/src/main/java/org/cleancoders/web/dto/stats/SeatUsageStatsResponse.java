package org.cleancoders.web.dto.stats;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "座位使用率统计响应")
public record SeatUsageStatsResponse(
        @Schema(description = "统计日期", example = "2025-01-15")
        String date,
        @Schema(description = "总座位数", example = "120")
        int totalSeats,
        @Schema(description = "已使用座位数", example = "85")
        int usedSeats,
        @Schema(description = "使用率", example = "0.708")
        double usageRate
) {
}
