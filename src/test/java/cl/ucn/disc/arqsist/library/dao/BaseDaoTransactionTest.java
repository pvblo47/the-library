/*
 * Copyright (c) 2026. Arquitectura de Sistemas, DISC, UCN, Antofagasta.
 */
package cl.ucn.disc.arqsist.library.dao;

import cl.ucn.disc.arqsist.library.db.Database;
import cl.ucn.disc.arqsist.library.model.Book;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests for {@link BaseDao#transaction(java.util.concurrent.Callable)} through the {@link BookDao} contract. */
class BaseDaoTransactionTest {

    /** In-memory database used by each test. */
    private Database database;

    /** DAO under test, typed as the interface that services will use. */
    private BookDao bookDao;

    /**
     * Creates a fresh in-memory database and a book DAO.
     *
     * @throws Exception if the database cannot be created
     */
    @BeforeEach
    void setUp() throws Exception {
        database = new Database("jdbc:sqlite::memory:");
        bookDao = new OrmLiteBookDao(database.connectionSource());
    }

    /**
     * Closes the database connection.
     *
     * @throws Exception if the connection cannot be closed
     */
    @AfterEach
    void tearDown() throws Exception {
        database.connectionSource().close();
    }

    /**
     * A successful transaction returns the callable value and keeps its writes.
     *
     * @throws Exception if the transaction fails
     */
    @Test
    void transactionReturnsTheCallableResultAndCommits() throws Exception {
        Book book = new Book("Refactoring", "Martin Fowler", "9780134757599", 3);

        int id = bookDao.transaction(() -> {
            bookDao.create(book);
            return book.getId();
        });

        assertTrue(id > 0);
        assertEquals(id, book.getId());
        assertNotNull(bookDao.findById(id));
    }

    /** A runtime exception thrown inside the transaction is rethrown unchanged, not hidden in a SQLException. */
    @Test
    void runtimeExceptionRemainsVisibleAfterTheTransactionFails() {
        IllegalStateException domainError = new IllegalStateException("domain error");

        IllegalStateException thrown = assertThrows(IllegalStateException.class, () ->
                bookDao.transaction(() -> {
                    throw domainError;
                }));

        assertSame(domainError, thrown);
    }

    /** A failed transaction rolls back the writes made before the failure. */
    @Test
    void failedTransactionRollsBackEarlierWrites() {
        Book book = new Book("Refactoring", "Martin Fowler", "9780134757599", 3);

        assertThrows(IllegalStateException.class, () ->
                bookDao.transaction(() -> {
                    bookDao.create(book);
                    throw new IllegalStateException("fail after the write");
                }));

        assertTrue(bookDao.findAll().isEmpty());
    }

    /** A SQLException thrown inside the transaction is rethrown as a SQLException. */
    @Test
    void sqlExceptionIsRethrownAsSqlException() {
        SQLException thrown = assertThrows(SQLException.class, () ->
                bookDao.transaction(() -> {
                    throw new SQLException("database failure");
                }));

        assertEquals("database failure", thrown.getMessage());
    }
}
