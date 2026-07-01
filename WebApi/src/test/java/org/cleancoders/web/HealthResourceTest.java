package org.cleancoders.web;

import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.cleancoders.web.filter.CorsFilter;
import org.cleancoders.web.resource.HealthResource;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for the health check endpoint.
 * Uses Jersey Test Framework with embedded Jetty HTTP server.
 */
public class HealthResourceTest extends JerseyTest
{

    @Override
    protected Application configure()
    {
        return new ResourceConfig(HealthResource.class, CorsFilter.class);
    }

    @Test
    void healthEndpointShouldReturn200WithStatusUP()
    {
        Response response = target("/health")
                .request(MediaType.APPLICATION_JSON)
                .get();

        assertEquals(200, response.getStatus());

        String body = response.readEntity(String.class);
        assertNotNull(body);
        assertTrue(body.contains("\"status\""));
        assertTrue(body.contains("\"UP\""));
        assertTrue(body.contains("\"timestamp\""));
    }

    @Test
    void healthEndpointShouldReturnJSONContentType()
    {
        Response response = target("/health")
                .request(MediaType.APPLICATION_JSON)
                .get();

        assertEquals(200, response.getStatus());
        assertTrue(response.getHeaderString("Content-Type").startsWith(MediaType.APPLICATION_JSON));
    }
}