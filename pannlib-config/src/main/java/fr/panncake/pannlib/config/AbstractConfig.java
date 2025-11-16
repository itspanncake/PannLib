package fr.panncake.pannlib.config;

import fr.panncake.pannlib.config.annotations.ConfigPath;
import fr.panncake.pannlib.config.annotations.DefaultValue;
import lombok.Getter;
import lombok.NonNull;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.representer.Representer;

import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Getter
public abstract class AbstractConfig {

    private final Path filePath;
    private Map<String, Object> configMap;
    private final Yaml yaml;

    public AbstractConfig(@NonNull Path filePath) {
        this.filePath = filePath;

        LoaderOptions loaderOptions = new LoaderOptions();
        DumperOptions dumperOptions = new DumperOptions();
        dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        dumperOptions.setPrettyFlow(true);

        this.yaml = new Yaml(new SafeConstructor(loaderOptions), new Representer(dumperOptions), dumperOptions);

        reload();
    }

    public void reload() {
        load();
        checkDefaults();
        save();
    }

    @SuppressWarnings("unchecked")
    private void load() {
        try {
            if (Files.notExists(filePath.getParent())) Files.createDirectories(filePath.getParent());

            if (Files.exists(filePath)) {
                try (InputStream in = Files.newInputStream(filePath)) {
                    Object loaded = yaml.load(in);
                    if (loaded instanceof Map<?, ?> map) {
                        this.configMap = (Map<String, Object>) map;
                    } else {
                        this.configMap = new LinkedHashMap<>();
                    }
                }
            } else {
                this.configMap = new LinkedHashMap<>();
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load config " + filePath, e);
        }
    }

    @SuppressWarnings("unchecked")
    private void checkDefaults() {
        Field[] fields = this.getClass().getDeclaredFields();

        for (Field field : fields) {
            if (!field.isAnnotationPresent(ConfigPath.class)) continue;

            String[] keys = field.getAnnotation(ConfigPath.class).value().split("\\.");
            Map<String, Object> currentMap = configMap;

            for (int i = 0; i < keys.length - 1; i++) {
                String key = keys[i];
                currentMap.putIfAbsent(key, new LinkedHashMap<String, Object>());
                Object next = currentMap.get(key);
                if (next instanceof Map<?, ?> map) {
                    currentMap = (Map<String, Object>) map;
                } else {
                    Map<String, Object> newMap = new LinkedHashMap<>();
                    currentMap.put(key, newMap);
                    currentMap = newMap;
                }
            }

            String lastKey = keys[keys.length - 1];
            Object rawValue = currentMap.get(lastKey);

            if (rawValue == null && field.isAnnotationPresent(DefaultValue.class)) {
                rawValue = field.getAnnotation(DefaultValue.class).value();
            }

            Object value = convertToFieldType(field, rawValue);

            currentMap.put(lastKey, value);
            try {
                field.setAccessible(true);
                field.set(this, value);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Failed to set config field " + field.getName(), e);
            }
        }
    }

    public void save() {
        try (Writer writer = Files.newBufferedWriter(filePath)) {
            yaml.dump(configMap, writer);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save config " + filePath, e);
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T getValue(String path) {
        String[] keys = path.split("\\.");
        Map<String, Object> currentMap = configMap;
        Object value = null;

        for (int i = 0; i < keys.length; i++) {
            value = currentMap.get(keys[i]);
            if (i < keys.length - 1) {
                if (value instanceof Map<?, ?> map) {
                    currentMap = (Map<String, Object>) map;
                } else {
                    return null;
                }
            }
        }
        return (T) value;
    }

    @SuppressWarnings("unchecked")
    public void setValue(String path, Object value) {
        String[] keys = path.split("\\.");
        Map<String, Object> currentMap = configMap;

        for (int i = 0; i < keys.length - 1; i++) {
            currentMap.putIfAbsent(keys[i], new LinkedHashMap<String, Object>());
            currentMap = (Map<String, Object>) currentMap.get(keys[i]);
        }

        currentMap.put(keys[keys.length - 1], value);
        save();
    }

    private Object convertToFieldType(Field field, Object value) {
        if (value == null) return null;

        Class<?> type = field.getType();

        try {
            if (type == Boolean.class || type == boolean.class) {
                if (value instanceof Boolean b) return b;
                return Boolean.parseBoolean(value.toString());
            } else if (type == Integer.class || type == int.class) {
                if (value instanceof Number n) return n.intValue();
                return Integer.parseInt(value.toString());
            } else if (type == Long.class || type == long.class) {
                if (value instanceof Number n) return n.longValue();
                return Long.parseLong(value.toString());
            } else if (type == Double.class || type == double.class) {
                if (value instanceof Number n) return n.doubleValue();
                return Double.parseDouble(value.toString());
            } else if (type == String.class) {
                return value.toString();
            } else if (type == List.class) {
                if (value instanceof List<?> list) return list;
                return Collections.singletonList(value.toString());
            } else if (type == Map.class) {
                if (value instanceof Map<?, ?> map) return map;
                return Map.of();
            } else {
                return value;
            }
        } catch (Exception e) {
            return null;
        }
    }
}
