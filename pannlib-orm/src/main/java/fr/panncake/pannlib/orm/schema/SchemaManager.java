package fr.panncake.pannlib.orm.schema;

import fr.panncake.pannlib.orm.connection.ConnectionManager;
import fr.panncake.pannlib.orm.exception.DatabaseException;
import fr.panncake.pannlib.orm.mapping.EntityMetadata;
import fr.panncake.pannlib.orm.query.QueryBuilder;

import java.sql.*;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public record SchemaManager(ConnectionManager connectionManager) {

    public <T> void ensureTable(Class<T> entityClass) {
        EntityMetadata metadata = new EntityMetadata(entityClass);
        try (Connection conn = connectionManager.getConnection()) {
            conn.setAutoCommit(true);
            if (!tableExists(conn, metadata.getTableName())) {
                String createSql = QueryBuilder.buildCreateTable(metadata);
                try (Statement s = conn.createStatement()) {
                    s.execute(createSql);
                }
            } else {
                Set<String> existing = getExistingColumns(conn, metadata.getTableName());
                for (String col : metadata.getColumnFields().keySet()) {
                    if (!existing.contains(col)) {
                        String alter = QueryBuilder.buildAddColumn(metadata, col);
                        try (Statement s = conn.createStatement()) {
                            s.execute(alter);
                        }
                    }
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to ensure table " + metadata.getTableName(), e);
        }
    }

    public <T> void ensureTables(Collection<Class<T>> entities) {
        for (Class<T> e : entities) {
            ensureTable(e);
        }
    }

    private boolean tableExists(Connection conn, String tableName) throws SQLException {
        DatabaseMetaData md = conn.getMetaData();
        try (ResultSet rs = md.getTables(null, null, tableName, new String[]{"TABLE"})) {
            if (rs.next()) return true;
        }
        try (ResultSet rs2 = md.getTables(null, null, tableName.toUpperCase(), new String[]{"TABLE"})) {
            return rs2.next();
        }
    }

    private Set<String> getExistingColumns(Connection conn, String tableName) throws SQLException {
        DatabaseMetaData md = conn.getMetaData();
        Set<String> cols = new HashSet<>();
        try (ResultSet rs = md.getColumns(null, null, tableName, null)) {
            while (rs.next()) {
                cols.add(rs.getString("COLUMN_NAME"));
            }
        }
        if (cols.isEmpty()) {
            try (ResultSet rs = md.getColumns(null, null, tableName.toUpperCase(), null)) {
                while (rs.next()) {
                    cols.add(rs.getString("COLUMN_NAME"));
                }
            }
        }
        return cols.stream().map(String::valueOf).collect(Collectors.toSet());
    }
}
