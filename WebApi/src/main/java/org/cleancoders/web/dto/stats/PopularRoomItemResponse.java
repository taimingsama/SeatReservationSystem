package org.cleancoders.web.dto.stats;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "热门自习室项")
public record PopularRoomItemResponse(
        @Schema(description = "自习室ID", example = "r1")
        String roomId,
        @Schema(description = "自习室名称", example = "A 自习室")
        String roomName,
        @Schema(description = "预约数量", example = "42")
        long reservationCount
) {
}
