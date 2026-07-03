package org.cleancoders.web.dto.seat;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "座位列表响应")
public record SeatListResponse(
        @Schema(description = "自习室ID")
        String roomId,
        @Schema(description = "自习室名称")
        String roomName,
        @Schema(description = "座位列表(可为空)")
        List<SeatResponse> seats
)
{
}