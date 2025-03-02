package com.lestora.common.data;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.function.Consumer;
import java.util.function.Function;

public class SQLiteManager {
    //public static final Logger LOGGER = LogManager.getLogger("lestora");
    private static final String DB_URL = "jdbc:sqlite:" + System.getProperty("user.home") + "/.lestoraRPG/lestora.db";
    private static Connection connection;

    public static void init() {
        try {
            String userHome = System.getProperty("user.home");
            File dbDir = new File(userHome, ".lestoraRPG");
            if (!dbDir.exists()) {
                dbDir.mkdirs();
            }

            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(DB_URL);

            connection.createStatement().execute("PRAGMA foreign_keys = ON");

            PlayerRepo.init();
            VillagerRepo.init();

        } catch (SQLException e) {
            //LOGGER.info("LestoraDB error: " + e.getMessage());
            //e.printStackTrace();
        } catch (ClassNotFoundException e) {
            //LOGGER.info("LESTORA Error: Could not find class for org.sqlite.JDBC: " + e.getMessage());
            //e.printStackTrace();
        }
    }

    public static synchronized <T> T withConn(Function<Connection, T> useConn) {
        return useConn.apply(connection);
    }

    public static synchronized void withConn(Consumer<Connection> useConn) {
        useConn.accept(connection);
    }
}