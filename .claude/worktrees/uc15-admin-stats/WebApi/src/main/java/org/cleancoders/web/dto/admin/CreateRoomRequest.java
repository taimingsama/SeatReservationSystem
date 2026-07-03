package org.cleancoders.web.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "创建自习室请求")
public record CreateRoomRequest(
        @Schema(description = "自习室名称", example = "自习室F")
        String name,
        @Schema(description = "位置", example = "综合楼二楼")
        String location,
        @Schema(description = "容量", example = "20")
        int capacity
)
{
}