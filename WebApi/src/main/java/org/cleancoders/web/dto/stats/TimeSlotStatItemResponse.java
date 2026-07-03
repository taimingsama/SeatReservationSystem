package org.cleancoders.web.dto.stats;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "时段统计项")
public record TimeSlotStatItemResponse(
        @Schema(description = "时段ID", example = "ts-1")
        String timeSlotId,
        @Schema(description = "时段标签", example = "上午 08:00-12:00")
        String label,
        @Schema(description = "预约数量", example = "35")
        long count
) {
}
