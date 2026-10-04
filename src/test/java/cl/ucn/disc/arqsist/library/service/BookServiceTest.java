/*
 * Copyright (c) 2026. Arquitectura de Sistemas, DISC, UCN, Antofagasta.
 */
package cl.ucn.disc.arqsist.library.service;

import cl.ucn.disc.arqsist.library.dao.BookDao;
import cl.ucn.disc.arqsist.library.dao.OrmLiteBookDao;
import cl.ucn.disc.arqsist.library.db.Database;
import cl.ucn.disc.arqsist.library.model.Book;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Tests for the inventory guard of {@link BookService}. */
class BookServiceTest {

    /** In-memory database used by each test. */
    private Database database;

    /** DAO used to check what was persisted. */
    private BookDao bookDao;

    /** Service under test. */
    private BookService service;

    /**
     * Creates a fresh in-memory database, a DAO and the service.
     *
     * @throws Exception if the database cannot be created
     */
    @BeforeEach
    void setUp() throws Exception {
        database = new Database("jdbc:sqlite::memory:");
        bookDao = new OrmLiteBookDao(database.connectionSource());
        service = new BookService(bookDao);
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

    /** Borrowing an available book decreases the count and persists it. */
    @Test
    void borrowDecreasesAndPersistsAvailableCopies() {
        Book book = persistedBook(2);

        service.borrow(book.getId());

        assertEquals(1, bookDao.findById(book.getId()).getAvailableCopies());
    }

    /** Borrowing a missing book fails with a not-found error. */
    @Test
    void borrowMissingBookThrowsNotFoundException() {
        NotFoundException thrown = assertThrows(NotFoundException.class, () -> service.borrow(9999));

        assertEquals("Book not found: 9999", thrown.getMessage());
    }

    /** Borrowing a book with no copies fails and the count never becomes negative. */
    @Test
    void borrowWithoutAvailableCopiesThrowsAndKeepsCountAtZero() {
        Book book = persistedBook(1);
        service.borrow(book.getId());

        IllegalStateException thrown = assertThrows(IllegalStateException.class,
                () -> service.borrow(book.getId()));

        assertEquals("No available copies of book " + book.getId(), thrown.getMessage());
        assertEquals(0, bookDao.findById(book.getId()).getAvailableCopies());
    }

    /** Returning a copy increases the count and persists it. */
    @Test
    void returnCopyIncreasesAndPersistsAvailableCopies() {
        Book book = persistedBook(2);
        service.borrow(book.getId());

        service.returnCopy(book.getId());

        assertEquals(2, bookDao.findById(book.getId()).getAvailableCopies());
    }

    /** Returning a copy of a missing book fails with a not-found error. */
    @Test
    void returnCopyOfMissingBookThrowsNotFoundException() {
        NotFoundException thrown = assertThrows(NotFoundException.class, () -> service.returnCopy(9999));

        assertEquals("Book not found: 9999", thrown.getMessage());
    }

    /**
     * Persists a book with all its copies available.
     *
     * @param copies number of copies of the book
     * @return the persisted book, with its generated identifier
     */
    private Book persistedBook(int copies) {
        Book book = new Book("Clean Code", "Robert C. Martin", "9780132350884", copies);
        bookDao.create(book);
        return book;
    }
}
