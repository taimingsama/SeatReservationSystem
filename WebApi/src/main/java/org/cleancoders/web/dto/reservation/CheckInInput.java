package org.cleancoders.web.dto.reservation;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "签到请求")
public record CheckInInput(
        @Schema(description = "6位签到码", example = "123456")
        String code
) {
}
