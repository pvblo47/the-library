package cl.ucn.disc.arqsist.library.dao;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.support.ConnectionSource;

import java.sql.SQLException;
import java.util.List;

/**
 * Shared ORMLite implementation of the generic CRUD operations.
 * Concrete DAOs only provide the entity class they manage.
 *
 * @param <T> entity type managed by the DAO
 */
public abstract class BaseDao<T> implements CrudDao<T> {

    protected final Dao<T, Integer> dao;

    protected BaseDao(ConnectionSource connectionSource, Class<T> clazz) {
        this.dao = execute(
                () -> DaoManager.createDao(connectionSource, clazz),
                "Error creating DAO for " + clazz.getSimpleName()
        );
    }

    @Override
    public List<T> findAll() {
        return execute(dao::queryForAll, "Error finding all entities");
    }

    @Override
    public T findById(Integer id) {
        return execute(() -> dao.queryForId(id), "Error finding entity by ID");
    }

    @Override
    public void create(T entity) {
        execute(() -> dao.create(entity), "Error creating entity");
    }

    @Override
    public void update(T entity) {
        execute(() -> dao.update(entity), "Error updating entity");
    }

    @Override
    public void delete(T entity) {
        execute(() -> dao.delete(entity), "Error deleting entity");
    }

    private static <R> R execute(SqlOperation<R> operation, String message) {
        try {
            return operation.run();
        } catch (SQLException e) {
            throw new RuntimeException(message, e);
        }
    }

    @FunctionalInterface
    private interface SqlOperation<R> {
        R run() throws SQLException;
    }
}
