package org.cleancoders.web.dto.reservation;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "重复预约响应")
public record DuplicateReservationResponse(
        @Schema(description = "错误信息", example = "该时段已有预约")
        String error,
        @Schema(description = "已存在的预约 ID", example = "res-existing-456")
        String existingReservationId
) {
}
