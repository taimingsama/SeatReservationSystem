package org.cleancoders.web.presenter;

import jakarta.inject.Singleton;
import jakarta.ws.rs.core.Response;

/**
 * Thread-local holder for the {@link Response} staged by the current
 * request's presenter call.
 * <p>
 * Registered as a CDI {@link Singleton} so every Resource and Presenter
 * shares the same instance. Resources call {@link #get()} to retrieve the
 * response; Presenters call {@link #set(Response)} to stage it.
 */
@Singleton
public class ResponseContext
{
    private final ThreadLocal<Response> current = new ThreadLocal<>();

    public void set(Response response)
    {
        current.set(response);
    }

    public Response get()
    {
        return current.get();
    }
}