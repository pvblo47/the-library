/*
 * Copyright (c) 2026. Arquitectura de Sistemas, DISC, UCN, Antofagasta.
 */
package cl.ucn.disc.arqsist.library.dao;

import cl.ucn.disc.arqsist.library.model.Book;
import com.j256.ormlite.support.ConnectionSource;

/**
 * ORMLite adapter for Book persistence.
 */
public final class OrmLiteBookDao extends BaseDao<Book> implements BookDao {

    /**
     * Creates the ORMLite adapter for Book persistence.
     *
     * @param connectionSource connection source of the database that holds the book table
     * @throws RuntimeException if the underlying ORMLite DAO cannot be created
     */
    public OrmLiteBookDao(ConnectionSource connectionSource) {
        super(connectionSource, Book.class);
    }
}
