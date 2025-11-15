package fr.panncake.pannlib.orm.query;

import fr.panncake.pannlib.orm.annotations.Column;
import fr.panncake.pannlib.orm.annotations.Id;
import fr.panncake.pannlib.orm.mapping.EntityMetadata;

import java.lang.reflect.Field;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public final class QueryBuilder {

    public static String buildCreateTable(EntityMetadata metadata) {
        StringBuilder sb = new StringBuilder();
        sb.append("CREATE TABLE IF NOT EXISTS ").append(metadata.getTableName()).append(" (\n");

        List<String> columnDefs = new ArrayList<>();

        metadata.getColumnFields().forEach((columnName, field) -> {
            StringBuilder col = new StringBuilder("  `" + columnName + "` " + sqlTypeFor(field));

            Column column = field.getAnnotation(Column.class);
            if (field.isAnnotationPresent(Id.class)) {
                Id id = field.getAnnotation(Id.class);
                if (!id.autoIncrement()) {
                    col.append(" NOT NULL");
                }
            }
            if (column != null) {
                if (!column.nullable()) col.append(" NOT NULL");
                if (column.unique()) col.append(" UNIQUE");
            }
            if (field.isAnnotationPresent(Id.class)) {
                Id id = field.getAnnotation(Id.class);
                if (id.autoIncrement()) {
                    col.append(" AUTO_INCREMENT");
                }
            }
            columnDefs.add(col.toString());
        });

        sb.append(String.join(",\n", columnDefs));

        if (!metadata.getPrimaryKeys().isEmpty()) {
            sb.append(",\n  PRIMARY KEY (");
            sb.append(metadata.getPrimaryKeys().stream().map(n -> "`" + n + "`").collect(java.util.stream.Collectors.joining(", ")));
            sb.append(")");
        }

        sb.append("\n) ENGINE=InnoDB;");
        return sb.toString();
    }

    public static String buildAddColumn(EntityMetadata metadata, String columnName) {
        Field field = metadata.getColumnFields().get(columnName);
        if (field == null) throw new IllegalArgumentException("Unknown column " + columnName);
        StringBuilder sb = new StringBuilder();
        sb.append("ALTER TABLE ").append(metadata.getTableName())
                .append(" ADD COLUMN `").append(columnName).append("` ").append(sqlTypeFor(field));

        Column column = field.getAnnotation(Column.class);
        if (column != null && !column.nullable()) sb.append(" NOT NULL");
        sb.append(";");
        return sb.toString();
    }

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

    private static String sqlTypeFor(Field field) {
        Class<?> type = field.getType();

        Column column = field.getAnnotation(Column.class);
        int length = column != null ? column.length() : 255;

        if (type == String.class) {
            return "VARCHAR(" + length + ")";
        }
        if (type == UUID.class) {
            return "CHAR(36)";
        }
        if (type == int.class || type == Integer.class) {
            return "INT";
        }
        if (type == long.class || type == Long.class) {
            return "BIGINT";
        }
        if (type == boolean.class || type == Boolean.class) {
            return "TINYINT(1)";
        }
        if (type == double.class || type == Double.class) {
            return "DOUBLE";
        }
        if (type == float.class || type == Float.class) {
            return "FLOAT";
        }
        if (type == byte[].class) {
            return "BLOB";
        }
        if (type == Instant.class || type == LocalDateTime.class) {
            return "TIMESTAMP";
        }
        if (type == java.time.LocalDate.class) {
            return "DATE";
        }
        if (type == java.time.LocalTime.class) {
            return "TIME";
        }

        return "TEXT";
    }
}
