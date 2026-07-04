package org.cleancoders.infrastructure.persistence.mysql;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.cleancoders.seatandroom.domain.Seat;
import org.cleancoders.seatandroom.domain.SeatStatus;
import org.cleancoders.seatandroom.outbound.SeatRepository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Singleton
public class MysqlSeatRepo implements SeatRepository {

    @Inject
    MysqlConnectionProvider cp;

    @Override
    public Optional<Seat> findByRoomIdAndSeatId(String roomId, int seatId) {
        String sql = "SELECT * FROM seat WHERE room_id = ? AND id = ?";
        try (Connection c = cp.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, roomId);
            ps.setInt(2, seatId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return Optional.empty();
    }

    @Override
    public Seat save(Seat seat) {
        String sql = "INSERT INTO seat (id, room_id, status) VALUES (?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE status = ?";
        try (Connection c = cp.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, seat.id());
            ps.setString(2, seat.roomId());
            ps.setString(3, seat.status().name());
            ps.setString(4, seat.status().name());
            ps.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException(e); }
        return seat;
    }

    @Override
    public List<Seat> findByRoomId(String roomId) {
        List<Seat> list = new ArrayList<>();
        String sql = "SELECT * FROM seat WHERE room_id = ? ORDER BY id";
        try (Connection c = cp.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, roomId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return list;
    }

    @Override
    public List<Seat> findAll() {
        List<Seat> list = new ArrayList<>();
        String sql = "SELECT * FROM seat";
        try (Connection c = cp.getConnection(); Statement s = c.createStatement(); ResultSet rs = s.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) { throw new RuntimeException(e); }
        return list;
    }

    @Override
    public void deleteByRoomId(String roomId) {
        String sql = "DELETE FROM seat WHERE room_id = ?";
        try (Connection c = cp.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, roomId);
            ps.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    private Seat mapRow(ResultSet rs) throws SQLException {
        return new Seat(
                rs.getInt("id"),
                rs.getString("room_id"),
                SeatStatus.valueOf(rs.getString("status"))
        );
    }
}
