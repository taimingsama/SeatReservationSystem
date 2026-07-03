package org.cleancoders.web.dto.seat;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "座位不存在响应")
public record SeatNotFoundInRoomResponse(
        @Schema(description = "错误信息", example = "座位不存在")
        String error,
        @Schema(description = "自习室ID", example = "r1")
        String roomId,
        @Schema(description = "座位ID", example = "1")
        int seatId
) {
}
