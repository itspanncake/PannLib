package fr.panncake.pannlib.orm.mapping;

import fr.panncake.pannlib.orm.annotations.*;
import lombok.Getter;

import java.lang.reflect.Field;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@Getter
public class EntityMetadata {
    private final Class<?> entityClass;
    private final String tableName;
    private final Field idField;
    private final Map<String, Field> columnFields;
    private final Map<Field, String> fieldToColumn;

    public EntityMetadata(Class<?> entityClass) {
        if (!entityClass.isAnnotationPresent(Entity.class)) {
            throw new IllegalArgumentException("Class must be annotated with @Entity");
        }

        this.entityClass = entityClass;
        this.tableName = resolveTableName(entityClass);
        this.idField = findIdField(entityClass);
        this.columnFields = new LinkedHashMap<>();
        this.fieldToColumn = new IdentityHashMap<>();

        scanFields(entityClass);
    }

    private String resolveTableName(Class<?> clazz) {
        Entity entity = clazz.getAnnotation(Entity.class);
        String name = entity.tableName();
        return name.isEmpty() ? clazz.getSimpleName().toLowerCase() : name;
    }

    private Field findIdField(Class<?> clazz) {
        for (Field field : clazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(Id.class)) {
                field.setAccessible(true);
                return field;
            }
        }
        throw new IllegalArgumentException("No @Id field found in " + clazz.getName());
    }

    private void scanFields(Class<?> clazz) {
        for (Field field : clazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(Transient.class)) {
                continue;
            }
            if (field.isAnnotationPresent(Column.class) || field.isAnnotationPresent(Id.class)) {
                field.setAccessible(true);
                String columnName = resolveColumnName(field);
                columnFields.put(columnName, field);
                fieldToColumn.put(field, columnName);
            }
        }
    }

    private String resolveColumnName(Field field) {
        Column column = field.getAnnotation(Column.class);
        if (column != null && !column.name().isEmpty()) {
            return column.name();
        }
        return field.getName();
    }

    public boolean isAutoIncrementId() {
        return idField.getAnnotation(Id.class).autoIncrement();
    }
}
