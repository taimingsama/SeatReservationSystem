package org.cleancoders.web.dto.reservation;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "座位不存在响应")
public record SeatNotFoundResponse(
        @Schema(description = "错误信息", example = "座位不存在")
        String error,
        @Schema(description = "座位 ID", example = "seat-nonexistent")
        String seatId
) {
}
