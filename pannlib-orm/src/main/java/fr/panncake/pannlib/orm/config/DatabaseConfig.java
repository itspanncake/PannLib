package fr.panncake.pannlib.orm.config;

import lombok.Builder;

@Builder
public record DatabaseConfig(DatabaseType type, String host, int port, String database, String username,
                             String password, int maxPoolSize, long connectionTimeout, boolean autoCommit) {
    public String getJdbcUrl() {
        return switch (type) {
            case MYSQL -> String.format("jdbc:mysql://%s:%d/%s?useSSL=false&allowPublicKeyRetrieval=true", host, port, database);
            case POSTGRESQL -> String.format("jdbc:postgresql://%s:%d/%s", host, port, database);
            case SQLITE -> String.format("jdbc:sqlite:%s", database);
        };
    }
}
