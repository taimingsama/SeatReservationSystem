package org.cleancoders.web.dto.room;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "自习室名称冲突响应")
public record RoomNameConflictResponse(
        @Schema(description = "错误信息", example = "自习室名称已存在")
        String error,
        @Schema(description = "名称", example = "A 自习室")
        String name
) {
}
