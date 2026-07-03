package org.cleancoders.web.resource;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.time.Instant;
import java.util.Map;

/**
 * Health check endpoint.
 * GET /api/health returns service status.
 */
@Path("/health")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Health", description = "服务健康检查")
public class HealthResource
{

    @GET
    @Operation(summary = "健康检查", description = "返回服务运行状态和时间戳。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "服务正常运行")
    })
    public Response health()
    {
        return Response.ok(Map.of(
                "status", "UP",
                "timestamp", Instant.now().toString()
        )).build();
    }
}