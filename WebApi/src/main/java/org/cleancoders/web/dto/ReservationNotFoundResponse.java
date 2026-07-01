package org.cleancoders.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "预约不存在响应")
public record ReservationNotFoundResponse(
        @Schema(description = "错误信息", example = "预约不存在")
        String error,
        @Schema(description = "预约 ID", example = "res-unknown")
        String reservationId
) {
}
