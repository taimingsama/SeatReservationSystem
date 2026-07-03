package org.cleancoders.web.dto.room;

import io.swagger.v3.oas.annotations.media.Schema;
import org.cleancoders.seatandroom.domain.RoomStatus;

@Schema(description = "自习室信息")
public record RoomResponse(
        @Schema(description = "自习室ID", example = "r1")
        String id,
        @Schema(description = "名称", example = "A 自习室")
        String name,
        @Schema(description = "位置", example = "1号楼2层")
        String location,
        @Schema(description = "容量", example = "10")
        int capacity,
        @Schema(description = "状态", example = "OPEN", allowableValues = {"OPEN", "CLOSED", "MAINTENANCE"})
        RoomStatus status
)
{
}
