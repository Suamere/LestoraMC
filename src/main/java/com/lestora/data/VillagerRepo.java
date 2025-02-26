package com.lestora.data;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class VillagerRepo {
    public static void init() {
        SQLiteManager.withConn(conn -> {
            try {
                // Create table for villager_data (villager_uuid and name)
                String sqlVillagerData = "CREATE TABLE IF NOT EXISTS villager_data ("
                        + "villager_uuid TEXT PRIMARY KEY, "
                        + "name TEXT"
                        + ")";
                conn.createStatement().execute(sqlVillagerData);

                // Create table for villager_interaction
                String sqlVillagerInteraction = "CREATE TABLE IF NOT EXISTS villager_interaction ("
                        + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                        + "villager_uuid TEXT, "
                        + "player_uuid TEXT, "
                        + "chatRole INTEGER, "
                        + "value TEXT, "
                        + "FOREIGN KEY(villager_uuid) REFERENCES villager_data(villager_uuid)"
                        + ")";
                conn.createStatement().execute(sqlVillagerInteraction);
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return null;
        });
    }

    public static void addVillager(UUID villagerUUID, String name) {
        SQLiteManager.withConn(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT OR REPLACE INTO villager_data (villager_uuid, name) VALUES (?, ?)")) {
                ps.setString(1, villagerUUID.toString());
                ps.setString(2, name);
                ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return null;
        });
    }

    public static void addInteraction(UUID playerUUID, UUID villagerUUID, Collection<VillagerInteraction> interactions) {
        SQLiteManager.withConn(conn -> {
            String sql = "INSERT INTO villager_interaction (villager_uuid, player_uuid, chatRole, value) VALUES (?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (VillagerInteraction interaction : interactions) {
                    ps.setString(1, villagerUUID.toString());
                    ps.setString(2, playerUUID.toString());
                    // Save the ordinal of the enum as an integer.
                    ps.setInt(3, interaction.getType().ordinal());
                    ps.setString(4, interaction.getValue());
                    ps.addBatch();
                }
                ps.executeBatch();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return null;
        });
    }

    public static List<VillagerInteraction> getInteractions(UUID playerUUID, UUID villagerUUID) {
        return SQLiteManager.withConn(conn -> {
            List<VillagerInteraction> list = new ArrayList<>();
            String sql = "SELECT chatRole, value FROM villager_interaction WHERE villager_uuid = ? AND player_uuid = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, villagerUUID.toString());
                ps.setString(2, playerUUID.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        int roleOrdinal = rs.getInt("chatRole");
                        String value = rs.getString("value");
                        // Convert the integer back to the enum.
                        VillagerInteractionType type = VillagerInteractionType.values()[roleOrdinal];
                        list.add(new VillagerInteraction(type, value));
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return list;
        });
    }
}