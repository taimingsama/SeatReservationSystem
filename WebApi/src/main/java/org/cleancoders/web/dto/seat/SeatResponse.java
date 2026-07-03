package org.cleancoders.web.dto.seat;

import io.swagger.v3.oas.annotations.media.Schema;
import org.cleancoders.seatandroom.domain.SeatStatus;

@Schema(description = "座位信息")
public record SeatResponse(
        @Schema(description = "座位序号 (教室内 1-N)", example = "5")
        int id,
        @Schema(description = "所属教室ID", example = "room-1")
        String roomId,
        @Schema(description = "座位状态",
                allowableValues = {"AVAILABLE", "RESERVED", "OCCUPIED", "MAINTENANCE"})
        SeatStatus status
)
{
}
