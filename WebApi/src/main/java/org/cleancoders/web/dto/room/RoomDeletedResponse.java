package org.cleancoders.web.dto.room;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "自习室删除成功响应")
public record RoomDeletedResponse(
        @Schema(description = "提示信息", example = "自习室已删除")
        String message,
        @Schema(description = "自习室ID", example = "r1")
        String roomId
) {
}
