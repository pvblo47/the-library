/*
 * Copyright (c) 2026. Arquitectura de Sistemas, DISC, UCN, Antofagasta.
 */
package cl.ucn.disc.arqsist.library.dao;

import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Generic CRUD contract shared by all DAOs. It also exposes transaction support, so a service
 * can group several writes into one atomic unit without touching ORMLite directly.
 *
 * @param <T> entity type managed by the DAO
 */
public interface CrudDao<T> {

    /**
     * Reads every stored entity.
     *
     * @return all entities, possibly empty
     * @throws RuntimeException if the entities cannot be read
     */
    List<T> findAll();

    /**
     * Reads one entity by its identifier.
     *
     * @param id identifier of the entity
     * @return the entity, or {@code null} when no entity has that identifier
     * @throws RuntimeException if the entity cannot be read
     */
    T findById(Integer id);

    /**
     * Stores a new entity.
     *
     * @param entity entity to store
     * @throws RuntimeException if the entity cannot be stored
     */
    void create(T entity);

    /**
     * Updates a stored entity.
     *
     * @param entity entity with the new state
     * @throws RuntimeException if the entity cannot be updated
     */
    void update(T entity);

    /**
     * Deletes a stored entity.
     *
     * @param entity entity to delete
     * @throws RuntimeException if the entity cannot be deleted
     */
    void delete(T entity);

    /**
     * Runs the callable in one database transaction. The transaction commits when the callable
     * returns, and rolls back when it throws. A {@link RuntimeException} thrown by the callable,
     * such as a domain error, is rethrown unchanged after the rollback.
     *
     * @param callable work to run inside the transaction
     * @param <R>      type returned by the callable
     * @return the value returned by the callable
     * @throws SQLException     if the transaction fails for a database reason
     * @throws RuntimeException if the callable throws it; the original exception is rethrown
     */
    <R> R transaction(Callable<R> callable) throws SQLException;
}
