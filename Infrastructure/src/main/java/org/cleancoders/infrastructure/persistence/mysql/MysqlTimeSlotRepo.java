package org.cleancoders.infrastructure.persistence.mysql;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.cleancoders.seatandroom.domain.TimeSlot;
import org.cleancoders.seatandroom.outbound.TimeSlotRepository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Singleton
public class MysqlTimeSlotRepo implements TimeSlotRepository {

    @Inject
    MysqlConnectionProvider cp;

    @Override
    public Optional<TimeSlot> findById(String id) {
        String sql = "SELECT * FROM time_slot WHERE id = ?";
        try (Connection c = cp.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return Optional.empty();
    }

    @Override
    public List<TimeSlot> findAll() {
        List<TimeSlot> list = new ArrayList<>();
        String sql = "SELECT * FROM time_slot ORDER BY start_time";
        try (Connection c = cp.getConnection(); Statement s = c.createStatement(); ResultSet rs = s.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) { throw new RuntimeException(e); }
        return list;
    }

    private TimeSlot mapRow(ResultSet rs) throws SQLException {
        return new TimeSlot(
                rs.getString("id"),
                rs.getString("start_time"),
                rs.getString("end_time"),
                rs.getString("label")
        );
    }
}
