package org.cleancoders.web.dto.seat;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "非法座位状态响应")
public record InvalidSeatStatusResponse(
        @Schema(description = "错误信息", example = "非法座位状态")
        String error,
        @Schema(description = "自习室ID", example = "r1")
        String roomId,
        @Schema(description = "座位ID", example = "1")
        int seatId,
        @Schema(description = "输入的状态值", example = "BROKEN")
        String status
) {
}
