package org.cleancoders.web.dto.reservation;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "时段不存在响应")
public record TimeSlotNotFoundResponse(
        @Schema(description = "错误信息", example = "时段不存在")
        String error,
        @Schema(description = "时段 ID", example = "ts-nonexistent")
        String timeSlotId
) {
}
