package org.cleancoders.infrastructure.persistence.mysql;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.cleancoders.reservation.domain.Reservation;
import org.cleancoders.reservation.domain.ReservationStatus;
import org.cleancoders.reservation.outbound.ReservationRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.*;

@Singleton
public class MysqlReservationRepo implements ReservationRepository {

    @Inject
    MysqlConnectionProvider cp;

    @Override
    public Reservation save(Reservation r) {
        String id = r.id() != null ? r.id() : UUID.randomUUID().toString();
        String sql = "INSERT INTO reservation (id, user_id, room_id, seat_id, time_slot_id, date, status, created_at, check_in_at, check_out_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE status=?, check_in_at=?, check_out_at=?";
        try (Connection c = cp.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.setString(2, r.userId());
            ps.setString(3, r.roomId());
            ps.setInt(4, r.seatId());
            ps.setString(5, r.timeSlotId());
            ps.setDate(6, java.sql.Date.valueOf(r.date()));
            ps.setString(7, r.status().name());
            ps.setTimestamp(8, r.createdAt() != null ? java.sql.Timestamp.valueOf(r.createdAt()) : null);
            ps.setTimestamp(9, r.checkInAt() != null ? java.sql.Timestamp.valueOf(r.checkInAt()) : null);
            ps.setTimestamp(10, r.checkOutAt() != null ? java.sql.Timestamp.valueOf(r.checkOutAt()) : null);
            ps.setString(11, r.status().name());
            ps.setTimestamp(12, r.checkInAt() != null ? java.sql.Timestamp.valueOf(r.checkInAt()) : null);
            ps.setTimestamp(13, r.checkOutAt() != null ? java.sql.Timestamp.valueOf(r.checkOutAt()) : null);
            ps.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException(e); }
        // Return new Reservation with the saved id
        Reservation saved = new Reservation(id, r.userId(), r.roomId(), r.seatId(), r.timeSlotId(), r.date());
        // Restore mutable state
        if (r.status() == ReservationStatus.CHECKED_IN) saved.checkIn();
        if (r.status() == ReservationStatus.CHECKED_OUT) { saved.checkIn(); saved.checkOut(); }
        if (r.status() == ReservationStatus.CANCELLED) saved.cancel();
        if (r.status() == ReservationStatus.EXPIRED) saved.expire();
        saved.setId(id);
        return saved;
    }

    @Override
    public Optional<Reservation> findById(String id) {
        String sql = "SELECT * FROM reservation WHERE id = ?";
        try (Connection c = cp.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return Optional.empty();
    }

    @Override
    public Optional<Reservation> findByUserIdAndDateAndTimeSlotIdAndStatusIn(
            String userId, LocalDate date, String timeSlotId, Set<ReservationStatus> statuses) {
        String sql = "SELECT * FROM reservation WHERE user_id = ? AND date = ? AND time_slot_id = ? AND status IN (?)";
        List<String> statusNames = statuses.stream().map(Enum::name).toList();
        String placeholders = String.join(",", Collections.nCopies(statusNames.size(), "?"));
        sql = sql.replace("IN (?)", "IN (" + placeholders + ")");
        try (Connection c = cp.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, userId);
            ps.setDate(2, java.sql.Date.valueOf(date));
            ps.setString(3, timeSlotId);
            for (int i = 0; i < statusNames.size(); i++) {
                ps.setString(4 + i, statusNames.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return Optional.empty();
    }

    @Override
    public Optional<Reservation> findBySeatIdAndDateAndTimeSlotIdAndStatusIn(
            String roomId, int seatId, LocalDate date, String timeSlotId, Set<ReservationStatus> statuses) {
        String sql = "SELECT * FROM reservation WHERE room_id = ? AND seat_id = ? AND date = ? AND time_slot_id = ? AND status IN (?)";
        List<String> statusNames = statuses.stream().map(Enum::name).toList();
        String placeholders = String.join(",", Collections.nCopies(statusNames.size(), "?"));
        sql = sql.replace("IN (?)", "IN (" + placeholders + ")");
        try (Connection c = cp.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, roomId);
            ps.setInt(2, seatId);
            ps.setDate(3, java.sql.Date.valueOf(date));
            ps.setString(4, timeSlotId);
            for (int i = 0; i < statusNames.size(); i++) {
                ps.setString(5 + i, statusNames.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return Optional.empty();
    }

    @Override
    public List<Reservation> findByUserId(String userId) {
        List<Reservation> list = new ArrayList<>();
        String sql = "SELECT * FROM reservation WHERE user_id = ? ORDER BY created_at DESC";
        try (Connection c = cp.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return list;
    }

    @Override
    public List<Reservation> findBySeatIdAndStatusIn(String roomId, int seatId, Set<ReservationStatus> statuses) {
        List<Reservation> list = new ArrayList<>();
        List<String> statusNames = statuses.stream().map(Enum::name).toList();
        String placeholders = String.join(",", Collections.nCopies(statusNames.size(), "?"));
        String sql = "SELECT * FROM reservation WHERE room_id = ? AND seat_id = ? AND status IN (" + placeholders + ")";
        try (Connection c = cp.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, roomId);
            ps.setInt(2, seatId);
            for (int i = 0; i < statusNames.size(); i++) {
                ps.setString(3 + i, statusNames.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return list;
    }

    @Override
    public List<Reservation> findAll() {
        List<Reservation> list = new ArrayList<>();
        String sql = "SELECT * FROM reservation ORDER BY created_at DESC";
        try (Connection c = cp.getConnection(); Statement s = c.createStatement(); ResultSet rs = s.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) { throw new RuntimeException(e); }
        return list;
    }

    private Reservation mapRow(ResultSet rs) throws SQLException {
        String id = rs.getString("id");
        String userId = rs.getString("user_id");
        String roomId = rs.getString("room_id");
        int seatId = rs.getInt("seat_id");
        String timeSlotId = rs.getString("time_slot_id");
        LocalDate date = rs.getDate("date").toLocalDate();
        ReservationStatus status = ReservationStatus.valueOf(rs.getString("status"));

        Reservation r = new Reservation(id, userId, roomId, seatId, timeSlotId, date);

        // Restore mutable state. The constructor sets RESERVED, override if needed.
        if (status == ReservationStatus.CHECKED_IN) r.checkIn();
        else if (status == ReservationStatus.CHECKED_OUT) { r.checkIn(); r.checkOut(); }
        else if (status == ReservationStatus.CANCELLED) r.cancel();
        else if (status == ReservationStatus.EXPIRED) r.expire();

        // setId overwrites the auto-generated id
        r.setId(id);

        // Restore real timestamps from DB (domain methods above set them to now())
        java.sql.Timestamp checkInTs = rs.getTimestamp("check_in_at");
        if (checkInTs != null) r.setCheckInAt(checkInTs.toLocalDateTime());
        java.sql.Timestamp checkOutTs = rs.getTimestamp("check_out_at");
        if (checkOutTs != null) r.setCheckOutAt(checkOutTs.toLocalDateTime());

        return r;
    }
}
