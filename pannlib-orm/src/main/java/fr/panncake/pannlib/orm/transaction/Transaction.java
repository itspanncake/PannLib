package fr.panncake.pannlib.orm.transaction;

import fr.panncake.pannlib.orm.connection.ConnectionManager;
import fr.panncake.pannlib.orm.exception.DatabaseException;
import lombok.Getter;

import java.sql.Connection;
import java.sql.SQLException;

@Getter
public class Transaction implements AutoCloseable {
    private final Connection connection;
    private boolean active;
    private boolean committed;
    private boolean rolledBack;

    public Transaction() {
        try {
            this.connection = ConnectionManager.getInstance().getConnection();
            this.connection.setAutoCommit(false);
            this.active = true;
        } catch (SQLException e) {
            throw new DatabaseException("Impossible de démarrer la transaction", e);
        }
    }

    public void commit() {
        if (!active) {
            throw new IllegalStateException("Transaction déjà terminée");
        }
        try {
            connection.commit();
            committed = true;
        } catch (SQLException e) {
            throw new DatabaseException("Échec du commit", e);
        } finally {
            closeConnection();
        }
    }

    public void rollback() {
        if (!active) {
            return;
        }
        try {
            connection.rollback();
            rolledBack = true;
        } catch (SQLException e) {
            throw new DatabaseException("Échec du rollback", e);
        } finally {
            closeConnection();
        }
    }

    @Override
    public void close() {
        if (active && !committed && !rolledBack) {
            rollback();
        }
    }

    private void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.setAutoCommit(true);
                connection.close();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Erreur lors de la fermeture de la connexion", e);
        } finally {
            active = false;
        }
    }
}
