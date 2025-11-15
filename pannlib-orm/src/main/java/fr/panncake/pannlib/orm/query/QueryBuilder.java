package fr.panncake.pannlib.orm.query;

import fr.panncake.pannlib.orm.mapping.EntityMetadata;

import java.util.stream.Collectors;

public final class QueryBuilder {

    public static String buildInsert(EntityMetadata metadata) {
        String columns = metadata.getColumnFields().keySet().stream()
                .filter(col -> !metadata.isAutoIncrementId() || !col.equals(metadata.getFieldToColumn().get(metadata.getIdField())))
                .collect(Collectors.joining(", "));

        String placeholders = metadata.getColumnFields().values().stream()
                .filter(f -> !metadata.isAutoIncrementId() || f != metadata.getIdField())
                .map(f -> "?")
                .collect(Collectors.joining(", "));

        return String.format("INSERT INTO %s (%s) VALUES (%s)", metadata.getTableName(), columns, placeholders);
    }

    public static String buildSelectById(EntityMetadata metadata) {
        return String.format("SELECT * FROM %s WHERE %s = ?", metadata.getTableName(),
                metadata.getFieldToColumn().get(metadata.getIdField()));
    }

    public static String buildSelectAll(EntityMetadata metadata) {
        return String.format("SELECT * FROM %s", metadata.getTableName());
    }

    public static String buildUpdate(EntityMetadata metadata) {
        String setClause = metadata.getColumnFields().entrySet().stream()
                .filter(e -> e.getValue() != metadata.getIdField())
                .map(e -> e.getKey() + " = ?")
                .collect(Collectors.joining(", "));

        return String.format("UPDATE %s SET %s WHERE %s = ?", metadata.getTableName(), setClause,
                metadata.getFieldToColumn().get(metadata.getIdField()));
    }

    public static String buildDeleteById(EntityMetadata metadata) {
        return String.format("DELETE FROM %s WHERE %s = ?", metadata.getTableName(),
                metadata.getFieldToColumn().get(metadata.getIdField()));
    }
}
