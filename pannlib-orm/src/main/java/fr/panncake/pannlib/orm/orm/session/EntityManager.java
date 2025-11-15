package fr.panncake.pannlib.orm.orm.session;

import fr.panncake.pannlib.orm.connection.ConnectionManager;
import fr.panncake.pannlib.orm.exception.DatabaseException;
import fr.panncake.pannlib.orm.mapping.EntityMetadata;
import fr.panncake.pannlib.orm.query.QueryBuilder;
import fr.panncake.pannlib.orm.util.ReflectionUtils;
import fr.panncake.pannlib.orm.util.SqlTypeConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class EntityManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(EntityManager.class);
    private final ConnectionManager connectionManager;
    private final Map<Class<?>, EntityMetadata> metadataCache = new ConcurrentHashMap<>();
    private final Executor asyncExecutor = Executors.newFixedThreadPool(
            Math.max(2, Runtime.getRuntime().availableProcessors())
    );

    public EntityManager() {
        this.connectionManager = ConnectionManager.getInstance();
    }

    public <T> void persist(T entity) {
        executeInTransaction(conn -> {
            EntityMetadata metadata = getMetadata(entity.getClass());
            String sql = QueryBuilder.buildInsert(metadata);
            try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                int index = 1;
                for (Map.Entry<String, Field> entry : metadata.getColumnFields().entrySet()) {
                    if (entry.getKey().equals(metadata.getFieldToColumn().get(metadata.getIdField())) &&
                            metadata.isAutoIncrementId()) {
                        continue;
                    }
                    Field field = entry.getValue();
                    stmt.setObject(index++, field.get(entity));
                }
                stmt.executeUpdate();

                if (metadata.isAutoIncrementId()) {
                    try (ResultSet rs = stmt.getGeneratedKeys()) {
                        if (rs.next()) {
                            metadata.getIdField().set(entity, rs.getObject(1));
                        }
                    }
                }
            }
        });
    }

    public <T> CompletableFuture<Void> persistAsync(T entity) {
        return CompletableFuture.runAsync(() -> persist(entity), asyncExecutor);
    }

    public <T> T find(Class<T> entityClass, Object id) {
        return executeWithConnection(conn -> {
            EntityMetadata metadata = getMetadata(entityClass);
            String sql = QueryBuilder.buildSelectById(metadata);
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setObject(1, id);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return mapResultSetToEntity(rs, entityClass, metadata);
                    }
                    return null;
                }
            }
        });
    }

    public <T> CompletableFuture<T> findAsync(Class<T> entityClass, Object id) {
        return CompletableFuture.supplyAsync(() -> find(entityClass, id), asyncExecutor);
    }

    public <T> void update(T entity) {
        executeInTransaction(conn -> {
            EntityMetadata metadata = getMetadata(entity.getClass());
            String sql = QueryBuilder.buildUpdate(metadata);
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                int index = 1;
                for (Map.Entry<String, Field> entry : metadata.getColumnFields().entrySet()) {
                    if (entry.getKey().equals(metadata.getFieldToColumn().get(metadata.getIdField()))) {
                        continue;
                    }
                    stmt.setObject(index++, getFieldValue(entry.getValue(), entity));
                }
                stmt.setObject(index, getFieldValue(metadata.getIdField(), entity));
                stmt.executeUpdate();
            }
        });
    }

    public <T> void delete(T entity) {
        executeInTransaction(conn -> {
            EntityMetadata metadata = getMetadata(entity.getClass());
            String sql = QueryBuilder.buildDeleteById(metadata);
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setObject(1, getFieldValue(metadata.getIdField(), entity));
                stmt.executeUpdate();
            }
        });
    }

    public <T> List<T> findAll(Class<T> entityClass) {
        return executeWithConnection(conn -> {
            EntityMetadata metadata = getMetadata(entityClass);
            String sql = QueryBuilder.buildSelectAll(metadata);
            try (PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {

                List<T> results = new ArrayList<>();
                while (rs.next()) {
                    results.add(mapResultSetToEntity(rs, entityClass, metadata));
                }
                return results;
            }
        });
    }

    private <T> T mapResultSetToEntity(ResultSet rs, Class<T> clazz, EntityMetadata metadata) {
        try {
            T instance = ReflectionUtils.instantiate(clazz);
            for (Map.Entry<String, Field> entry : metadata.getColumnFields().entrySet()) {
                String columnName = entry.getKey();
                Field field = entry.getValue();
                Object value = SqlTypeConverter.fromSqlObject(rs, columnName, field.getType());
                if (value != null) {
                    ReflectionUtils.setFieldValue(field, instance, value);
                }
            }
            return instance;
        } catch (Exception e) {
            throw new DatabaseException("Échec du mapping ResultSet → entité", e);
        }
    }

    private Object getFieldValue(Field field, Object instance) {
        return ReflectionUtils.getFieldValue(field, instance);
    }

    private EntityMetadata getMetadata(Class<?> clazz) {
        return metadataCache.computeIfAbsent(clazz, EntityMetadata::new);
    }

    private void executeInTransaction(TransactionOperation operation) {
        try (Connection conn = connectionManager.getConnection()) {
            boolean autoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try {
                operation.execute(conn);
                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw new DatabaseException("Transaction failed", e);
            } finally {
                conn.setAutoCommit(autoCommit);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to execute transaction", e);
        }
    }

    private <T> T executeWithConnection(ConnectionFunction<T> function) {
        try (Connection conn = connectionManager.getConnection()) {
            return function.apply(conn);
        } catch (Exception e) {
            throw new DatabaseException("Database operation failed", e);
        }
    }

    @FunctionalInterface
    private interface TransactionOperation {
        void execute(Connection conn) throws Exception;
    }

    @FunctionalInterface
    private interface ConnectionFunction<T> {
        T apply(Connection conn) throws Exception;
    }
}
