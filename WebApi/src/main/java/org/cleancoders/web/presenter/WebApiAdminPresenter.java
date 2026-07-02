package org.cleancoders.web.presenter;

import jakarta.inject.Singleton;
import jakarta.ws.rs.core.Response;
import org.cleancoders.seatandroom.domain.StudyRoom;
import org.cleancoders.seatandroom.usecase.DeleteRoomUseCase;
import org.cleancoders.seatandroom.usecase.ManageRoomsUseCase;
import org.cleancoders.seatandroom.usecase.UpdateRoomUseCase;
import org.cleancoders.web.dto.room.RoomResponse;

import java.util.Map;

/**
 * WebApi presenter for admin use cases ({@link ManageRoomsUseCase}, {@link UpdateRoomUseCase}, {@link DeleteRoomUseCase}).
 * Extends {@link WebApiPresenter} to inherit auth error branches
 * (401 invalid token, 404 user not found, 403 forbidden).
 */
@Singleton
public class WebApiAdminPresenter extends WebApiPresenter implements
        ManageRoomsUseCase.Presenter,
        UpdateRoomUseCase.Presenter,
        DeleteRoomUseCase.Presenter
{

    @Override
    public void success(StudyRoom room)
    {
        responseContext.set(Response.status(201).entity(
                new RoomResponse(room.id(), room.name(), room.location(), room.capacity(), room.status())
        ).build());
    }

    @Override
    public void updateSuccess(StudyRoom room)
    {
        responseContext.set(Response.ok(
                new RoomResponse(room.id(), room.name(), room.location(), room.capacity(), room.status())
        ).build());
    }

    @Override
    public void roomNameAlreadyExists(String name)
    {
        responseContext.set(Response.status(409).entity(Map.of(
                "error", "自习室名称已存在",
                "name", name
        )).build());
    }

    @Override
    public void roomNotFound(String roomId)
    {
        responseContext.set(Response.status(404).entity(Map.of(
                "error", "自习室不存在",
                "roomId", roomId
        )).build());
    }

    @Override
    public void deleteSuccess(String roomId)
    {
        responseContext.set(Response.status(200).entity(Map.of(
                "message", "自习室已删除",
                "roomId", roomId
        )).build());
    }

    @Override
    public void roomAlreadyClosed(String roomId)
    {
        responseContext.set(Response.status(409).entity(Map.of(
                "error", "自习室已处于关闭状态",
                "roomId", roomId
        )).build());
    }
}