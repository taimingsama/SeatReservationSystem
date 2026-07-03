package org.cleancoders.reservation.domain;

/**
 * Represents the lifecycle status of a reservation.
 */
public enum ReservationStatus {
    RESERVED,
    CHECKED_IN,
    CHECKED_OUT,
    CANCELLED,
    EXPIRED
}
