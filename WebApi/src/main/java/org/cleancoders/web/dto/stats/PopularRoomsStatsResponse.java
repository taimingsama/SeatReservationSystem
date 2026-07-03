package org.cleancoders.web.dto.stats;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "热门自习室统计响应")
public record PopularRoomsStatsResponse(
        @Schema(description = "统计日期", example = "2025-01-15")
        String date,
        @Schema(description = "热门自习室列表")
        List<PopularRoomItemResponse> rooms
) {
}
