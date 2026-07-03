package org.cleancoders.web.dto.seat;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "时间段信息")
public record TimeSlotResponse(
        @Schema(description = "时段ID", example = "ts-1")
        String id,
        @Schema(description = "开始时间", example = "08:00")
        String startTime,
        @Schema(description = "结束时间", example = "12:00")
        String endTime,
        @Schema(description = "时段标签", example = "上午 08:00-12:00")
        String label
)
{
}
