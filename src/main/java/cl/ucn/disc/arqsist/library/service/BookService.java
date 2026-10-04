/*
 * Copyright (c) 2026. Arquitectura de Sistemas, DISC, UCN, Antofagasta.
 */
package cl.ucn.disc.arqsist.library.service;

import cl.ucn.disc.arqsist.library.dao.BookDao;
import cl.ucn.disc.arqsist.library.model.Book;

import java.util.List;

/** Owns the book catalog and is the single place that changes the number of available copies. */
public final class BookService {

    /** DAO used to read and persist books. */
    private final BookDao dao;

    /**
     * Creates a book service.
     *
     * @param dao DAO used to read and persist books
     */
    public BookService(BookDao dao) {
        this.dao = dao;
    }

    /**
     * Lists all books.
     *
     * @return all books in the catalog
     * @throws RuntimeException if the books cannot be read
     */
    public List<Book> listAll() {
        return dao.findAll();
    }

    /**
     * Finds one book by its identifier.
     *
     * @param id identifier of the book
     * @return the book, or {@code null} when no book has that identifier
     * @throws RuntimeException if the book cannot be read
     */
    public Book findById(int id) {
        return dao.findById(id);
    }

    /**
     * Adds a book to the catalog. All its copies start as available.
     *
     * @param book book to add
     * @return the persisted book, with its generated identifier
     * @throws RuntimeException if the book cannot be persisted
     */
    public Book create(Book book) {
        book.setAvailableCopies(book.getTotalCopies());
        dao.create(book);
        return book;
    }

    /**
     * Takes one copy of a book out of the available stock.
     *
     * @param bookId identifier of the book to borrow
     * @throws NotFoundException     if the book does not exist
     * @throws IllegalStateException if the book has no available copies
     * @throws RuntimeException      if a persistence operation fails
     */
    public void borrow(int bookId) {
        Book book = dao.findById(bookId);
        if (book == null) {
            throw new NotFoundException("Book not found: " + bookId);
        }
        if (book.getAvailableCopies() <= 0) {
            throw new IllegalStateException("No available copies of book " + bookId);
        }
        book.setAvailableCopies(book.getAvailableCopies() - 1);
        dao.update(book);
    }

    /**
     * Puts one copy of a book back into the available stock.
     *
     * @param bookId identifier of the book that is returned
     * @throws NotFoundException if the book does not exist
     * @throws RuntimeException  if a persistence operation fails
     */
    public void returnCopy(int bookId) {
        Book book = dao.findById(bookId);
        if (book == null) {
            throw new NotFoundException("Book not found: " + bookId);
        }
        book.setAvailableCopies(book.getAvailableCopies() + 1);
        dao.update(book);
    }
}
