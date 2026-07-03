package org.cleancoders.web.dto.room;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "自习室不存在响应")
public record RoomNotFoundResponse(
        @Schema(description = "错误信息", example = "自习室不存在")
        String error,
        @Schema(description = "自习室ID", example = "r1")
        String roomId
) {
}
