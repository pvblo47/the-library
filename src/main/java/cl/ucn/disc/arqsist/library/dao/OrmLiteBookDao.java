package cl.ucn.disc.arqsist.library.dao;

import cl.ucn.disc.arqsist.library.model.Book;
import com.j256.ormlite.support.ConnectionSource;

/**
 * ORMLite adapter for Book persistence.
 */
public final class OrmLiteBookDao extends BaseDao<Book> implements BookDao {

    public OrmLiteBookDao(ConnectionSource connectionSource) {
        super(connectionSource, Book.class);
    }
}
