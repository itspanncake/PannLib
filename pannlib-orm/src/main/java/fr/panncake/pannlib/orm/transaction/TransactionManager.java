package fr.panncake.pannlib.orm.transaction;

import fr.panncake.pannlib.orm.exception.DatabaseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TransactionManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(TransactionManager.class);
    private static final ThreadLocal<Transaction> CURRENT = new ThreadLocal<>();

    private TransactionManager() {}

    public static void requireTransaction(Runnable operation) {
        Transaction tx = CURRENT.get();
        if (tx == null) {
            try (Transaction newTx = new Transaction()) {
                CURRENT.set(newTx);
                try {
                    operation.run();
                    if (newTx.isActive()) {
                        newTx.commit();
                    }
                } catch (Exception e) {
                    if (newTx.isActive()) {
                        newTx.rollback();
                    }
                    throw e instanceof DatabaseException ? (DatabaseException) e : new DatabaseException("Transaction failed", e);
                } finally {
                    CURRENT.remove();
                }
            }
        } else {
            operation.run();
        }
    }

    public static <T> T requireTransaction(TransactionFunction<T> function) throws Exception {
        Transaction tx = CURRENT.get();
        if (tx == null) {
            try (Transaction newTx = new Transaction()) {
                CURRENT.set(newTx);
                try {
                    T result = function.apply(newTx);
                    if (newTx.isActive()) {
                        newTx.commit();
                    }
                    return result;
                } catch (Exception e) {
                    if (newTx.isActive()) {
                        newTx.rollback();
                    }
                    throw e instanceof DatabaseException ? (DatabaseException) e : new DatabaseException("Transaction failed", e);
                } finally {
                    CURRENT.remove();
                }
            }
        } else {
            return function.apply(tx);
        }
    }

    public static Transaction currentTransaction() {
        Transaction tx = CURRENT.get();
        if (tx == null) {
            throw new IllegalStateException("Any transaction found on this thread");
        }
        return tx;
    }

    @FunctionalInterface
    public interface TransactionFunction<T> {
        T apply(Transaction tx) throws Exception;
    }
}
