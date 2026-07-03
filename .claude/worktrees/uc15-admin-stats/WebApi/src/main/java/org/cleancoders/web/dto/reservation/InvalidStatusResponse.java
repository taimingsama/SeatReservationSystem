package org.cleancoders.web.dto.reservation;

import io.swagger.v3.oas.annotations.media.Schema;
import org.cleancoders.reservation.domain.ReservationStatus;

@Schema(description = "状态不允许操作响应")
public record InvalidStatusResponse(
        @Schema(description = "错误信息", example = "当前状态不允许签到")
        String error,
        @Schema(description = "当前预约状态", example = "CHECKED_IN",
                allowableValues = {"RESERVED", "CHECKED_IN", "CHECKED_OUT", "CANCELLED", "EXPIRED"})
        ReservationStatus currentStatus
) {
}
