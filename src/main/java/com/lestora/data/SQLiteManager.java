package com.lestora.data;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.UUID;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class SQLiteManager {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final String DB_URL = "jdbc:sqlite:" + System.getProperty("user.home") + "/.lestoraRPG/lestora.db";
    private static Connection connection;

    public static void init() {
        try {
            // Create the directory if it doesn't exist.
            String userHome = System.getProperty("user.home");
            File dbDir = new File(userHome, ".lestoraRPG");
            if (!dbDir.exists()) {
                dbDir.mkdirs();
            }

            Class.forName("org.sqlite.JDBC");
            // Establish connection
            connection = DriverManager.getConnection(DB_URL);
            // Create table if it doesn't exist
            String sql = "CREATE TABLE IF NOT EXISTS player_data ("
                    + "player_uuid TEXT PRIMARY KEY, "
                    + "swimLevel INTEGER"
                    + ")";
            connection.createStatement().execute(sql);
        } catch (SQLException e) {
            LOGGER.info("ERROR CREATING DATABASE");
            LOGGER.info(e.getMessage());
        } catch (ClassNotFoundException e) {
            LOGGER.info("LESTORA Error: Could not find class for org.sqlite.JDBC: " + e.getMessage());
        }
    }

    public static void setSwimLevel(UUID uuid, int level) {
        String sql = "INSERT OR REPLACE INTO player_data (player_uuid, swimLevel) VALUES (?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.setInt(2, level);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static Integer getSwimLevel(UUID uuid) {
        String sql = "SELECT swimLevel FROM player_data WHERE player_uuid = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
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
    }
}