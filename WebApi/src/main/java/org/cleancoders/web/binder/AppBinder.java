package org.cleancoders.web.binder;

import org.glassfish.hk2.utilities.binding.AbstractBinder;

/**
 * HK2 dependency injection binder.
 * Binds outbound interface implementations from Infrastructure module
 * to their corresponding interfaces defined in business modules.
 *
 * Binding rules:
 * - bind(Implementation.class).to(Interface.class);    — create new instance per injection
 * - bind(Implementation.class).to(Interface.class).in(Singleton.class); — singleton
 */
public class AppBinder extends AbstractBinder {

    @Override
    protected void configure() {
        // === UserAndAuth ===
        // bind(InMemoryUserRepo.class).to(UserRepository.class);

        // === SeatAndRoom ===
        // bind(InMemorySeatRepo.class).to(SeatRepository.class);

        // === Reservation ===
        // bind(InMemoryReservationRepo.class).to(ReservationRepository.class);

        // === SystemTask ===
        // bind(InMemoryTaskRepo.class).to(TaskRepository.class);
    }
}
