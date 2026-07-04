package org.cleancoders.infrastructure.persistence.mysql;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.cleancoders.userandauth.domain.User;
import org.cleancoders.userandauth.domain.UserRole;
import org.cleancoders.userandauth.outbound.UserRepository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Singleton
public class MysqlUserRepo implements UserRepository {

    @Inject
    MysqlConnectionProvider cp;

    @Override
    public Optional<User> findByUsername(String username) {
        String sql = "SELECT * FROM `user` WHERE username = ?";
        try (Connection c = cp.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return Optional.empty();
    }

    @Override
    public Optional<User> findById(String id) {
        String sql = "SELECT * FROM `user` WHERE id = ?";
        try (Connection c = cp.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return Optional.empty();
    }

    @Override
    public User save(User user) {
        String id = user.id() != null ? user.id() : UUID.randomUUID().toString();
        String sql = "INSERT INTO `user` (id, username, password, role, name, email, " +
                "reservation_count, study_seconds, check_in_count, credit_score, banned) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE username=?, password=?, role=?, name=?, email=?, " +
                "reservation_count=?, study_seconds=?, check_in_count=?, credit_score=?, banned=?";
        try (Connection c = cp.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            int i = 1;
            ps.setString(i++, id);
            ps.setString(i++, user.username());
            ps.setString(i++, user.password());
            ps.setString(i++, user.role().name());
            ps.setString(i++, user.name());
            ps.setString(i++, user.email());
            ps.setInt(i++, user.reservationCount());
            ps.setInt(i++, user.studySeconds());
            ps.setInt(i++, user.checkInCount());
            ps.setInt(i++, user.creditScore());
            ps.setBoolean(i++, user.banned());
            // ON DUPLICATE KEY UPDATE
            ps.setString(i++, user.username());
            ps.setString(i++, user.password());
            ps.setString(i++, user.role().name());
            ps.setString(i++, user.name());
            ps.setString(i++, user.email());
            ps.setInt(i++, user.reservationCount());
            ps.setInt(i++, user.studySeconds());
            ps.setInt(i++, user.checkInCount());
            ps.setInt(i++, user.creditScore());
            ps.setBoolean(i++, user.banned());
            ps.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException(e); }
        return new User(id, user.username(), user.password(), user.role(), user.name(), user.email(),
                user.reservationCount(), user.studySeconds(), user.checkInCount(), user.creditScore(), user.banned());
    }

    @Override
    public List<User> findAll() {
        List<User> list = new ArrayList<>();
        String sql = "SELECT * FROM `user`";
        try (Connection c = cp.getConnection(); Statement s = c.createStatement(); ResultSet rs = s.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) { throw new RuntimeException(e); }
        return list;
    }

    @Override
    public void deleteById(String id) {
        String sql = "DELETE FROM `user` WHERE id = ?";
        try (Connection c = cp.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    private User mapRow(ResultSet rs) throws SQLException {
        return new User(
                rs.getString("id"),
                rs.getString("username"),
                rs.getString("password"),
                UserRole.valueOf(rs.getString("role")),
                rs.getString("name"),
                rs.getString("email"),
                rs.getInt("reservation_count"),
                rs.getInt("study_seconds"),
                rs.getInt("check_in_count"),
                rs.getInt("credit_score"),
                rs.getBoolean("banned")
        );
    }
}
