package org.cleancoders.web.dto.seat;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "非法状态转换响应")
public record InvalidStatusTransitionResponse(
        @Schema(description = "错误信息", example = "非法状态转换")
        String error,
        @Schema(description = "自习室ID", example = "r1")
        String roomId,
        @Schema(description = "座位ID", example = "1")
        int seatId,
        @Schema(description = "当前状态", example = "IN_USE")
        String currentStatus,
        @Schema(description = "目标状态", example = "AVAILABLE")
        String targetStatus
) {
}
