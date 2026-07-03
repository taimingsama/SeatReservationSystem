package org.cleancoders.web.dto.seat;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "时间段列表响应")
public record TimeSlotListResponse(
        @Schema(description = "时间段列表")
        List<TimeSlotResponse> timeSlots
)
{
}
