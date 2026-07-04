package org.cleancoders.infrastructure.persistence.mysql;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.cleancoders.seatandroom.domain.RoomLayout;
import org.cleancoders.seatandroom.domain.RoomStatus;
import org.cleancoders.seatandroom.domain.StudyRoom;
import org.cleancoders.seatandroom.outbound.RoomRepository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Singleton
public class MysqlRoomRepo implements RoomRepository {

    @Inject
    MysqlConnectionProvider cp;

    @Override
    public List<StudyRoom> findByStatus(RoomStatus status) {
        List<StudyRoom> list = new ArrayList<>();
        String sql = "SELECT * FROM study_room WHERE status = ?";
        try (Connection c = cp.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, status.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return list;
    }

    @Override
    public Optional<StudyRoom> findById(String id) {
        String sql = "SELECT * FROM study_room WHERE id = ?";
        try (Connection c = cp.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return Optional.empty();
    }

    @Override
    public StudyRoom save(StudyRoom room) {
        String id = room.id() != null ? room.id() : UUID.randomUUID().toString();
        String sql = "INSERT INTO study_room (id, name, location, layout, status) VALUES (?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE name=?, location=?, layout=?, status=?";
        try (Connection c = cp.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.setString(2, room.name());
            ps.setString(3, room.location());
            ps.setString(4, room.layout().name());
            ps.setString(5, room.status().name());
            ps.setString(6, room.name());
            ps.setString(7, room.location());
            ps.setString(8, room.layout().name());
            ps.setString(9, room.status().name());
            ps.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException(e); }
        return new StudyRoom(id, room.name(), room.location(), room.layout(), room.status());
    }

    @Override
    public Optional<StudyRoom> findByName(String name) {
        String sql = "SELECT * FROM study_room WHERE name = ?";
        try (Connection c = cp.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return Optional.empty();
    }

    @Override
    public List<StudyRoom> findAll() {
        List<StudyRoom> list = new ArrayList<>();
        String sql = "SELECT * FROM study_room";
        try (Connection c = cp.getConnection(); Statement s = c.createStatement(); ResultSet rs = s.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) { throw new RuntimeException(e); }
        return list;
    }

    private StudyRoom mapRow(ResultSet rs) throws SQLException {
        return new StudyRoom(
                rs.getString("id"),
                rs.getString("name"),
                rs.getString("location"),
                RoomLayout.valueOf(rs.getString("layout")),
                RoomStatus.valueOf(rs.getString("status"))
        );
    }
}
