package org.cleancoders.web.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "更新用户信用分请求")
public record UpdateCreditRequest(
        @Schema(description = "新的信用分", example = "60")
        int creditScore
)
{
}
