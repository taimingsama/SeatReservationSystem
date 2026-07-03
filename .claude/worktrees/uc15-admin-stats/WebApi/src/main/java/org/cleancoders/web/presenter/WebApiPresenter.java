package org.cleancoders.web.presenter;

import jakarta.inject.Inject;

/**
 * Base class for all WebApi presenter implementations.
 * <p>
 * Holds a {@link ResponseContext} that is shared between the presenter
 * (write side) and the Resource (read side). The field is initialised with
 * a default instance so unit tests can work without a CDI container;
 * the {@code @Inject} annotation lets CDI replace it with the singleton.
 */
public abstract class WebApiPresenter
{
    @Inject
    public ResponseContext responseContext;
}
