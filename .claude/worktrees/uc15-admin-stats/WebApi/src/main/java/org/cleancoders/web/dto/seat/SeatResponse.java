package org.cleancoders.web.dto.seat;

import io.swagger.v3.oas.annotations.media.Schema;
import org.cleancoders.common_reservation_seatAndRoom.domain.SeatStatus;

@Schema(description = "座位信息")
public record SeatResponse(
        @Schema(description = "座位ID", example = "seat-1")
        String id,
        @Schema(description = "座位编号", example = "A-1")
        String seatNumber,
        @Schema(description = "座位状态",
                allowableValues = {"AVAILABLE", "RESERVED", "OCCUPIED", "MAINTENANCE"})
        SeatStatus status
)
{
}