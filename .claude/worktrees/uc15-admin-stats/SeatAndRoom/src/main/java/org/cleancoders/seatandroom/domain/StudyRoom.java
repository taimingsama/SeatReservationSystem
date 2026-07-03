package org.cleancoders.seatandroom.domain;

/**
 * A study room that contains seats.
 */
public record StudyRoom(
        String id,
        String name,
        String location,
        int capacity,
        RoomStatus status
)
{
}
