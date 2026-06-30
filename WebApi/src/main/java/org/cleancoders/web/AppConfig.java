package org.cleancoders.web;

import jakarta.ws.rs.core.Application;
import org.cleancoders.web.filter.CorsFilter;
import org.cleancoders.web.resource.HealthResource;

import java.util.HashSet;
import java.util.Set;

/**
 * JAX-RS Application configuration.
 * Registers all resource classes and providers.
 * Path mapping is handled by web.xml, so no @ApplicationPath annotation here.
 */
public class AppConfig extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> classes = new HashSet<>();
        // Resources
        classes.add(HealthResource.class);
        // Providers / Filters
        classes.add(CorsFilter.class);
        return classes;
    }
}