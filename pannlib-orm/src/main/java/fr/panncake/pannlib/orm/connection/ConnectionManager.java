package fr.panncake.pannlib.orm.connection;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import fr.panncake.pannlib.orm.config.DatabaseConfig;
import fr.panncake.pannlib.orm.exception.DatabaseException;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;

@Getter
public final class ConnectionManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectionManager.class);
    private static ConnectionManager instance;
    private final HikariDataSource dataSource;

    private ConnectionManager(DatabaseConfig config) {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(config.getJdbcUrl());
        hikariConfig.setUsername(config.username());
        hikariConfig.setPassword(config.password());
        hikariConfig.setMaximumPoolSize(config.maxPoolSize());
        hikariConfig.setConnectionTimeout(config.connectionTimeout());
        hikariConfig.setAutoCommit(config.autoCommit());
        hikariConfig.setDriverClassName(config.type().getDriverClass());

        this.dataSource = new HikariDataSource(hikariConfig);
        LOGGER.info("Connection pool initialized for {}", config.type());
    }

    public static synchronized void initialize(DatabaseConfig config) {
        if (instance != null) {
            throw new IllegalStateException("ConnectionManager already initialized");
        }
        instance = new ConnectionManager(config);
    }

    public static ConnectionManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("ConnectionManager not initialized");
        }
        return instance;
    }

    public Connection getConnection() throws SQLException {
        try {
            return dataSource.getConnection();
        } catch (SQLException e) {
            throw new DatabaseException("Failed to obtain connection", e);
        }
    }

    public void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            LOGGER.info("Connection pool shut down");
        }
    }
}
