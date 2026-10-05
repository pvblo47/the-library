/*
 * Copyright (c) 2026. Arquitectura de Sistemas, DISC, UCN, Antofagasta.
 */
package cl.ucn.disc.arqsist.library.model;

import cl.ucn.disc.arqsist.library.db.LocalDatePersister;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.time.LocalDate;

/** A request by a member to borrow a book later. */
@DatabaseTable(tableName = "reservations")
public final class Reservation {

    /** Generated identifier of the reservation. */
    @DatabaseField(generatedId = true)
    private int id;

    /** Member who reserved the book. */
    @DatabaseField(canBeNull = false, foreign = true, foreignAutoRefresh = true)
    private Member member;

    /** Book that was reserved. */
    @DatabaseField(canBeNull = false, foreign = true, foreignAutoRefresh = true)
    private Book book;

    /** Day the reservation was made. */
    @DatabaseField(canBeNull = false, persisterClass = LocalDatePersister.class)
    private LocalDate reservedAt;

    /** Whether the reservation has been turned into a loan. */
    @DatabaseField
    private boolean fulfilled;

    /** Creates an empty reservation. ORMLite uses it to build reservations read from the database. */
    public Reservation() {
    }

    /**
     * Creates a pending reservation.
     *
     * @param member     member who reserves the book
     * @param book       book that is reserved
     * @param reservedAt day the reservation is made
     */
    public Reservation(Member member, Book book, LocalDate reservedAt) {
        this.member = member;
        this.book = book;
        this.reservedAt = reservedAt;
        this.fulfilled = false;
    }

    /**
     * Returns the identifier of the reservation.
     *
     * @return the identifier
     */
    public int getId() {
        return id;
    }

    /**
     * Sets the identifier of the reservation.
     *
     * @param id the identifier
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * Returns the member who reserved the book.
     *
     * @return the member
     */
    public Member getMember() {
        return member;
    }

    /**
     * Sets the member who reserved the book.
     *
     * @param member the member
     */
    public void setMember(Member member) {
        this.member = member;
    }

    /**
     * Returns the reserved book.
     *
     * @return the book
     */
    public Book getBook() {
        return book;
    }

    /**
     * Sets the reserved book.
     *
     * @param book the book
     */
    public void setBook(Book book) {
        this.book = book;
    }

    /**
     * Returns the day the reservation was made.
     *
     * @return the reservation date
     */
    public LocalDate getReservedAt() {
        return reservedAt;
    }

    /**
     * Sets the day the reservation was made.
     *
     * @param reservedAt the reservation date
     */
    public void setReservedAt(LocalDate reservedAt) {
        this.reservedAt = reservedAt;
    }

    /**
     * Tells whether the reservation has been turned into a loan.
     *
     * @return {@code true} if the reservation is fulfilled
     */
    public boolean isFulfilled() {
        return fulfilled;
    }

    /**
     * Marks the reservation as fulfilled or pending.
     *
     * @param fulfilled {@code true} if the reservation is fulfilled
     */
    public void setFulfilled(boolean fulfilled) {
        this.fulfilled = fulfilled;
    }
}
