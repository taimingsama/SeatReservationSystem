package org.cleancoders.web.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "创建座位请求")
public record CreateSeatRequest(
        @Schema(description = "所属自习室ID", example = "room-1")
        String roomId,
        @Schema(description = "座位编号", example = "A-9")
        String seatNumber
)
{
}