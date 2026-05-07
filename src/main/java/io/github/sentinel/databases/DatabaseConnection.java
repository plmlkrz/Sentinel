package io.github.sentinel.databases;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.github.sentinel.configurations.Configuration;

/**
 * Manages JDBC database connections for test verification steps.
 * Configure in sentinel.yml:
 * <pre>
 *   db.mydb.url: jdbc:postgresql://localhost:5432/testdb
 *   db.mydb.username: testuser
 *   db.mydb.password: testpass
 * </pre>
 * Connections are scoped per thread to support parallel test execution.
 * No JDBC driver is bundled — add your driver (MySQL, PostgreSQL, etc.) to your project's pom.xml.
 */
public class DatabaseConnection {

    private static final Logger log = LogManager.getLogger(DatabaseConnection.class);
    private static final ThreadLocal<Connection> connectionHolder = new ThreadLocal<>();

    private DatabaseConnection() {
    }

    /**
     * Opens a connection to the named database using sentinel.yml config.
     * @param name String the database config name (e.g., "mydb" maps to db.mydb.url etc.)
     * @return Connection the open JDBC connection
     */
    public static Connection getConnection(String name) {
        Connection conn = connectionHolder.get();
        if (conn != null) {
            try {
                if (!conn.isClosed()) return conn;
            } catch (SQLException ignored) {
            }
        }

        String url = Configuration.toString("db." + name + ".url");
        String user = Configuration.toString("db." + name + ".username");
        String password = Configuration.toString("db." + name + ".password");

        if (url == null) {
            throw new IllegalStateException(
                    "No database URL configured. Add 'db." + name + ".url' to sentinel.yml.");
        }

        try {
            conn = DriverManager.getConnection(url, user, password);
            connectionHolder.set(conn);
            log.debug("Connected to database: {}", name);
            return conn;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to connect to database '" + name + "': " + e.getMessage(), e);
        }
    }

    /**
     * Returns the currently active connection for this thread, or throws if none is open.
     */
    public static Connection requireConnection() {
        Connection conn = connectionHolder.get();
        if (conn == null) {
            throw new IllegalStateException("No database connection is open. Use 'I connect to the database named X' first.");
        }
        return conn;
    }

    /**
     * Executes a query and returns the first column of the first row as a String.
     * @param sql String the SQL query to execute
     * @return String the result, or null if no rows returned
     */
    public static String querySingleValue(String sql) {
        try (Statement stmt = requireConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getString(1);
            return null;
        } catch (SQLException e) {
            throw new RuntimeException("Query failed: " + sql, e);
        }
    }

    /**
     * Returns the number of rows matching a query.
     * @param sql String a COUNT(*) query or similar
     * @return int the row count
     */
    public static int queryRowCount(String sql) {
        try (Statement stmt = requireConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
            return 0;
        } catch (SQLException e) {
            throw new RuntimeException("Row count query failed: " + sql, e);
        }
    }

    /**
     * Closes the current thread's database connection.
     */
    public static void closeConnection() {
        Connection conn = connectionHolder.get();
        if (conn != null) {
            try {
                conn.close();
                log.debug("Database connection closed.");
            } catch (SQLException ignored) {
            }
            connectionHolder.remove();
        }
    }
}
