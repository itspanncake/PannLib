package fr.panncake.pannlib.orm.orm.config;

import lombok.Getter;

@Getter
public enum DatabaseType {
    MYSQL("com.mysql.cj.jdbc.Driver"),
    POSTGRESQL("org.postgresql.Driver"),
    SQLITE("org.sqlite.JDBC");

    private final String driverClass;

    DatabaseType(String driverClass) {
        this.driverClass = driverClass;
    }

}
