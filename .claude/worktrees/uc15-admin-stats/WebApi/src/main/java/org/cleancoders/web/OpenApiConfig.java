package org.cleancoders.web;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.servers.Server;

/**
 * OpenAPI 注解配置。
 * 为生成的 openapi.json 提供 API 基本信息（标题、版本、描述等）。
 * 无需方法实现 — 注解由 Swagger Core 在运行时扫描。
 */
@OpenAPIDefinition(
        servers = {
                @Server(url = "./")
        }
)
public class OpenApiConfig
{
    /* 空类 — 仅承载类级别的 OpenAPI 注解，由 Swagger Core 扫描识别 */
}
