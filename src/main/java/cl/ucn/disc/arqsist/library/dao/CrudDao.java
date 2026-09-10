package cl.ucn.disc.arqsist.library.dao;

import java.util.List;

/**
 * Generic CRUD contract shared by all DAOs.
 *
 * @param <T> entity type managed by the DAO
 */
public interface CrudDao<T> {

    List<T> findAll();

    T findById(Integer id);

    void create(T entity);

    void update(T entity);

    void delete(T entity);
}
