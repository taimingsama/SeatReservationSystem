package org.cleancoders.web;

import io.swagger.v3.jaxrs2.integration.resources.AcceptHeaderOpenApiResource;
import io.swagger.v3.jaxrs2.integration.resources.OpenApiResource;
import jakarta.ws.rs.core.Application;
import org.cleancoders.web.binder.ReservationBinder;
import org.cleancoders.web.binder.SeatAndRoomBinder;
import org.cleancoders.web.binder.UserAndAuthBinder;
import org.cleancoders.web.binder.WebAppBinder;
import org.cleancoders.web.filter.CorsFilter;
import org.cleancoders.web.resource.*;

import java.util.HashSet;
import java.util.Set;

/**
 * JAX-RS Application configuration.
 * Registers all resource classes, providers, and HK2 binders.
 * Path mapping is handled by web.xml, so no @ApplicationPath annotation here.
 */
public class AppConfig extends Application
{

    @Override
    public Set<Class<?>> getClasses()
    {
        Set<Class<?>> classes = new HashSet<>();
        // Resources
        classes.add(HealthResource.class);
        classes.add(AuthResource.class);
        classes.add(ReservationResource.class);
        classes.add(RoomResource.class);
        classes.add(AdminResource.class);
        // Swagger / OpenAPI endpoints (served under /api/ prefix per web.xml mapping)
        classes.add(OpenApiConfig.class);
        classes.add(OpenApiResource.class);
        classes.add(AcceptHeaderOpenApiResource.class);
        // Providers / Filters
        classes.add(CorsFilter.class);
        // HK2 Binder (must be registered as a class so Jersey discovers it)
        classes.add(WebAppBinder.class);
        classes.add(ReservationBinder.class);
        classes.add(SeatAndRoomBinder.class);
        classes.add(UserAndAuthBinder.class);
        return classes;
    }
}