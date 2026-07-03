package org.cleancoders.web.dto.stats;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "爽约率统计响应")
public record NoShowRateStatsResponse(
        @Schema(description = "统计日期", example = "2025-01-15")
        String date,
        @Schema(description = "总预约数", example = "100")
        int totalReservations,
        @Schema(description = "已过期数", example = "12")
        int expired,
        @Schema(description = "爽约率", example = "0.12")
        double noShowRate
) {
}
