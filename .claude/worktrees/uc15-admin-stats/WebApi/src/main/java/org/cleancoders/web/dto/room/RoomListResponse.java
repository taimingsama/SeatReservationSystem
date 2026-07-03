package org.cleancoders.web.dto.room;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "自习室列表响应")
public record RoomListResponse(
        @Schema(description = "OPEN 状态自习室列表(可为空)")
        List<RoomResponse> rooms
)
{
}
