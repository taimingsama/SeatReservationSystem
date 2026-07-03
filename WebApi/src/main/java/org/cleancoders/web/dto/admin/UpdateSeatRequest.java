package org.cleancoders.web.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "更新座位状态请求")
public record UpdateSeatRequest(
        @Schema(description = "目标状态", example = "MAINTENANCE",
                allowableValues = {"AVAILABLE", "MAINTENANCE"})
        String status
)
{
}
