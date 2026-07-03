package org.cleancoders.web.dto.room;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "自习室已关闭响应")
public record RoomAlreadyClosedResponse(
        @Schema(description = "错误信息", example = "自习室已处于关闭状态")
        String error,
        @Schema(description = "自习室ID", example = "r1")
        String roomId
) {
}
