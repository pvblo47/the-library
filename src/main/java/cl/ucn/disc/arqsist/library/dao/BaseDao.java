/*
 * Copyright (c) 2026. Arquitectura de Sistemas, DISC, UCN, Antofagasta.
 */
package cl.ucn.disc.arqsist.library.dao;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.misc.TransactionManager;
import com.j256.ormlite.support.ConnectionSource;

import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Shared ORMLite implementation of the generic CRUD operations and of transactions.
 * Concrete DAOs only provide the entity class they manage.
 *
 * @param <T> entity type managed by the DAO
 */
public abstract class BaseDao<T> implements CrudDao<T> {

    /** ORMLite DAO that performs the real database operations. */
    protected final Dao<T, Integer> dao;

    /**
     * Creates the ORMLite DAO for an entity class.
     *
     * @param connectionSource connection source of the database
     * @param clazz            entity class managed by this DAO
     * @throws RuntimeException if the ORMLite DAO cannot be created
     */
    protected BaseDao(ConnectionSource connectionSource, Class<T> clazz) {
        this.dao = execute(
                () -> DaoManager.createDao(connectionSource, clazz),
                "Error creating DAO for " + clazz.getSimpleName()
        );
    }

    /**
     * {@inheritDoc}
     *
     * @return all entities, possibly empty
     * @throws RuntimeException if the entities cannot be read
     */
    @Override
    public List<T> findAll() {
        return execute(dao::queryForAll, "Error finding all entities");
    }

    /**
     * {@inheritDoc}
     *
     * @param id identifier of the entity
     * @return the entity, or {@code null} when no entity has that identifier
     * @throws RuntimeException if the entity cannot be read
     */
    @Override
    public T findById(Integer id) {
        return execute(() -> dao.queryForId(id), "Error finding entity by ID");
    }

    /**
     * {@inheritDoc}
     *
     * @param entity entity to store
     * @throws RuntimeException if the entity cannot be stored
     */
    @Override
    public void create(T entity) {
        execute(() -> dao.create(entity), "Error creating entity");
    }

    /**
     * {@inheritDoc}
     *
     * @param entity entity with the new state
     * @throws RuntimeException if the entity cannot be updated
     */
    @Override
    public void update(T entity) {
        execute(() -> dao.update(entity), "Error updating entity");
    }

    /**
     * {@inheritDoc}
     *
     * @param entity entity to delete
     * @throws RuntimeException if the entity cannot be deleted
     */
    @Override
    public void delete(T entity) {
        execute(() -> dao.delete(entity), "Error deleting entity");
    }

    /**
     * {@inheritDoc}
     *
     * <p>ORMLite wraps any non-SQL exception thrown by the callable in a {@link SQLException}.
     * When that cause is a {@link RuntimeException}, it is rethrown so a domain error (for example
     * a not-found error) is not hidden behind a database error.
     *
     * @param callable work to run inside the transaction
     * @param <R>      type returned by the callable
     * @return the value returned by the callable
     * @throws SQLException     if the transaction fails and the cause is not a runtime exception
     * @throws RuntimeException if the callable throws it; the original exception is rethrown
     */
    @Override
    public <R> R transaction(Callable<R> callable) throws SQLException {
        try {
            return TransactionManager.callInTransaction(dao.getConnectionSource(), callable);
        } catch (SQLException e) {
            if (e.getCause() instanceof RuntimeException cause) {
                throw cause;
            }
            throw e;
        }
    }

    /**
     * Runs a SQL operation and converts its {@link SQLException} into a {@link RuntimeException}.
     *
     * @param operation SQL operation to run
     * @param message   message of the runtime exception
     * @param <R>       type returned by the operation
     * @return the value returned by the operation
     * @throws RuntimeException if the operation throws a {@link SQLException}; the SQL error is its cause
     */
    private static <R> R execute(SqlOperation<R> operation, String message) {
        try {
            return operation.run();
        } catch (SQLException e) {
            throw new RuntimeException(message, e);
        }
    }

    /**
     * A database operation that may throw a {@link SQLException}.
     *
     * @param <R> type returned by the operation
     */
    @FunctionalInterface
    private interface SqlOperation<R> {

        /**
         * Runs the operation.
         *
         * @return the result of the operation
         * @throws SQLException if the database operation fails
         */
        R run() throws SQLException;
    }
}
