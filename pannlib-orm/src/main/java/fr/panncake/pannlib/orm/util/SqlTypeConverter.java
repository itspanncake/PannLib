package fr.panncake.pannlib.orm.util;

import fr.panncake.pannlib.orm.exception.DatabaseException;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Date;
import java.util.UUID;

public final class SqlTypeConverter {

    private SqlTypeConverter() {}

    public static Object toSqlObject(Object javaValue, Class<?> targetType) {
        if (javaValue == null) return null;

        if (targetType == String.class) return javaValue.toString();
        if (targetType == Integer.class || targetType == int.class) return ((Number) javaValue).intValue();
        if (targetType == Long.class || targetType == long.class) return ((Number) javaValue).longValue();
        if (targetType == Boolean.class || targetType == boolean.class) return javaValue;
        if (targetType == Double.class || targetType == double.class) return ((Number) javaValue).doubleValue();
        if (targetType == Float.class || targetType == float.class) return ((Number) javaValue).floatValue();
        if (targetType == BigDecimal.class) return javaValue instanceof BigDecimal ? javaValue : new BigDecimal(javaValue.toString());
        if (targetType == Date.class) return new Date(((Date) javaValue).getTime());
        if (targetType == Timestamp.class) {
            if (javaValue instanceof LocalDateTime) {
                return Timestamp.valueOf((LocalDateTime) javaValue);
            } else if (javaValue instanceof Instant) {
                return Timestamp.from((Instant) javaValue);
            } else if (javaValue instanceof Date) {
                return new Timestamp(((Date) javaValue).getTime());
            }
        }
        if (targetType == Time.class && javaValue instanceof LocalTime) {
            return Time.valueOf((LocalTime) javaValue);
        }
        if (targetType == java.sql.Date.class && javaValue instanceof LocalDate) {
            return java.sql.Date.valueOf((LocalDate) javaValue);
        }
        if (targetType == UUID.class) return javaValue.toString();
        if (targetType == byte[].class) return javaValue;

        throw new DatabaseException("Type not supported for SQL conversion: " + javaValue.getClass());
    }

    public static <T> T fromSqlObject(ResultSet rs, String columnName, Class<T> targetType) throws SQLException {
        Object value = rs.getObject(columnName);
        if (value == null) return null;

        if (targetType == String.class) return targetType.cast(value.toString());
        if (targetType == Integer.class || targetType == int.class) return targetType.cast(((Number) value).intValue());
        if (targetType == Long.class || targetType == long.class) return targetType.cast(((Number) value).longValue());
        if (targetType == Boolean.class || targetType == boolean.class) return targetType.cast(value);
        if (targetType == Double.class || targetType == double.class) return targetType.cast(((Number) value).doubleValue());
        if (targetType == Float.class || targetType == float.class) return targetType.cast(((Number) value).floatValue());
        if (targetType == BigDecimal.class) return targetType.cast(value);
        if (targetType == LocalDateTime.class) {
            Timestamp ts = rs.getTimestamp(columnName);
            return ts == null ? null : targetType.cast(ts.toLocalDateTime());
        }
        if (targetType == LocalDate.class) {
            java.sql.Date d = rs.getDate(columnName);
            return d == null ? null : targetType.cast(d.toLocalDate());
        }
        if (targetType == LocalTime.class) {
            Time t = rs.getTime(columnName);
            return t == null ? null : targetType.cast(t.toLocalTime());
        }
        if (targetType == Instant.class) {
            Timestamp ts = rs.getTimestamp(columnName);
            return ts == null ? null : targetType.cast(ts.toInstant());
        }
        if (targetType == UUID.class) {
            String s = rs.getString(columnName);
            return s == null ? null : targetType.cast(UUID.fromString(s));
        }
        if (targetType == byte[].class) return targetType.cast(value);

        throw new DatabaseException("Type not supported for SQL conversion: " + targetType);
    }
}
