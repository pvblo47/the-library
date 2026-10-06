/*
 * Copyright (c) 2026. Arquitectura de Sistemas, DISC, UCN, Antofagasta.
 */
package cl.ucn.disc.arqsist.library.model;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

/** A title of the catalog, with the number of copies the library owns and the copies that are available. */
@DatabaseTable(tableName = "books")
public final class Book {

    /** Generated identifier of the book. */
    @DatabaseField(generatedId = true)
    private int id;

    /** Title of the book. */
    @DatabaseField(canBeNull = false)
    private String title;

    /** Author of the book. */
    @DatabaseField(canBeNull = false)
    private String author;

    /** ISBN of the book. */
    @DatabaseField(canBeNull = false)
    private String isbn;

    /** Number of copies the library owns. */
    @DatabaseField(canBeNull = false)
    private int totalCopies;

    /** Number of copies that are not on loan. */
    @DatabaseField(canBeNull = false)
    private int availableCopies;

    /** Creates an empty book. ORMLite uses it to build books read from the database. */
    public Book() {
    }

    /**
     * Creates a book whose copies are all available.
     *
     * @param title       title of the book
     * @param author      author of the book
     * @param isbn        ISBN of the book
     * @param totalCopies number of copies the library owns, all of them available
     */
    public Book(String title, String author, String isbn, int totalCopies) {
        this.title = title;
        this.author = author;
        this.isbn = isbn;
        this.totalCopies = totalCopies;
        this.availableCopies = totalCopies;
    }

    /**
     * Returns the identifier of the book.
     *
     * @return the identifier
     */
    public int getId() {
        return id;
    }

    /**
     * Sets the identifier of the book.
     *
     * @param id the identifier
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * Returns the title of the book.
     *
     * @return the title
     */
    public String getTitle() {
        return title;
    }

    /**
     * Sets the title of the book.
     *
     * @param title the title
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Returns the author of the book.
     *
     * @return the author
     */
    public String getAuthor() {
        return author;
    }

    /**
     * Sets the author of the book.
     *
     * @param author the author
     */
    public void setAuthor(String author) {
        this.author = author;
    }

    /**
     * Returns the ISBN of the book.
     *
     * @return the ISBN
     */
    public String getIsbn() {
        return isbn;
    }

    /**
     * Sets the ISBN of the book.
     *
     * @param isbn the ISBN
     */
    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

    /**
     * Returns the number of copies the library owns.
     *
     * @return the total copies
     */
    public int getTotalCopies() {
        return totalCopies;
    }

    /**
     * Sets the number of copies the library owns.
     *
     * @param totalCopies the total copies
     */
    public void setTotalCopies(int totalCopies) {
        this.totalCopies = totalCopies;
    }

    /**
     * Returns the number of copies that are not on loan.
     *
     * @return the available copies
     */
    public int getAvailableCopies() {
        return availableCopies;
    }

    /**
     * Sets the number of copies that are not on loan.
     *
     * @param availableCopies the available copies
     */
    public void setAvailableCopies(int availableCopies) {
        this.availableCopies = availableCopies;
    }
}
