package org.cleancoders.web.dto.reservation;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "预约列表项")
public record ReservationItemResponse(
        @Schema(description = "预约ID", example = "res-001")
        String reservationId,
        @Schema(description = "自习室ID", example = "r1")
        String roomId,
        @Schema(description = "自习室名称", example = "A 自习室")
        String roomName,
        @Schema(description = "座位ID", example = "1")
        int seatId,
        @Schema(description = "时段ID", example = "ts-1")
        String timeSlotId,
        @Schema(description = "时段标签", example = "上午 08:00-12:00")
        String timeSlotLabel,
        @Schema(description = "日期", example = "2025-01-15")
        String date,
        @Schema(description = "预约状态", example = "RESERVED")
        String status,
        @Schema(description = "创建时间", example = "2025-01-14T20:30:00Z")
        String createdAt
) {
}
