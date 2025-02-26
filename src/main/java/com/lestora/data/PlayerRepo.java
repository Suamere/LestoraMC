package com.lestora.data;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.*;
import java.util.UUID;

public class PlayerRepo {
    private static final Logger LOGGER = LogManager.getLogger();

    public static void init() {
        SQLiteManager.withConn(conn -> {
            try {
            String sql = "CREATE TABLE IF NOT EXISTS player_data ("
                    + "player_uuid TEXT PRIMARY KEY, "
                    + "swimLevel INTEGER"
                    + ")";
            conn.createStatement().execute(sql);
            } catch (SQLException e) {
                LOGGER.info("LestoraDB error: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    public static void setSwimLevel(UUID uuid, int level) {
        SQLiteManager.withConn(conn -> {
            String sql = "INSERT OR REPLACE INTO player_data (player_uuid, swimLevel) VALUES (?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                ps.setInt(2, level);
                ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    public static Integer getSwimLevel(UUID uuid) {
        return SQLiteManager.withConn(conn -> {
            String sql = "SELECT swimLevel FROM player_data WHERE player_uuid = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt("swimLevel");
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            setSwimLevel(uuid, 1);
            return 1;
        });
    }
}