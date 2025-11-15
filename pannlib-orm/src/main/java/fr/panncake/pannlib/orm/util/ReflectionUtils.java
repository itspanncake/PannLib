package fr.panncake.pannlib.orm.util;

import fr.panncake.pannlib.orm.exception.DatabaseException;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Objects;

public final class ReflectionUtils {

    private ReflectionUtils() {}

    public static <T> T instantiate(Class<T> clazz) {
        try {
            Constructor<T> constructor = clazz.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (NoSuchMethodException e) {
            throw new DatabaseException("Aucun constructeur sans paramètre pour " + clazz.getName(), e);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new DatabaseException("Échec de l'instanciation de " + clazz.getName(), e);
        }
    }

    public static Object getFieldValue(Field field, Object instance) {
        try {
            field.setAccessible(true);
            return field.get(instance);
        } catch (IllegalAccessException e) {
            throw new DatabaseException("Impossible d'accéder au champ " + field.getName(), e);
        }
    }

    public static void setFieldValue(Field field, Object instance, Object value) {
        try {
            field.setAccessible(true);
            field.set(instance, value);
        } catch (IllegalAccessException e) {
            throw new DatabaseException("Impossible de définir le champ " + field.getName(), e);
        }
    }

    public static Object[] createSnapshot(Object entity, Field[] fields) {
        Object[] snapshot = new Object[fields.length];
        for (int i = 0; i < fields.length; i++) {
            snapshot[i] = getFieldValue(fields[i], entity);
        }
        return snapshot;
    }

    public static boolean isDirty(Object entity, Object[] snapshot, Field[] fields) {
        for (int i = 0; i < fields.length; i++) {
            Object current = getFieldValue(fields[i], entity);
            Object original = snapshot[i];
            if (!Objects.equals(current, original)) {
                return true;
            }
        }
        return false;
    }
}
