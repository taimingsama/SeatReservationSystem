package org.cleancoders.web.binder;

import jakarta.inject.Singleton;
import org.cleancoders.web.presenter.*;
import org.glassfish.hk2.utilities.binding.AbstractBinder;

/**
 * HK2 dependency injection binder.
 * Binds outbound interface implementations from Infrastructure module
 * to their corresponding interfaces defined in business modules.
 * <p>
 * Binding rules:
 * - bind(Implementation.class).to(Interface.class); — create new instance per injection (PerLookup)
 * - bind(instance).to(Contract.class); — share the same instance across contracts
 */
public class WebAppBinder extends AbstractBinder
{

    @Override
    protected void configure()
    {
        bind(ResponseContext.class).to(ResponseContext.class).in(Singleton.class);
    }
}
