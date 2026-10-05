/*
 * Copyright (c) 2026. Arquitectura de Sistemas, DISC, UCN, Antofagasta.
 */
package cl.ucn.disc.arqsist.library.model;

import cl.ucn.disc.arqsist.library.db.LocalDatePersister;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.time.LocalDate;

/** A book that a member has borrowed. */
@DatabaseTable(tableName = "loans")
public final class Loan {

    /** Generated identifier of the loan. */
    @DatabaseField(generatedId = true)
    private int id;

    /** Member who borrowed the book. */
    @DatabaseField(canBeNull = false, foreign = true, foreignAutoRefresh = true)
    private Member member;

    /** Book that was borrowed. */
    @DatabaseField(canBeNull = false, foreign = true, foreignAutoRefresh = true)
    private Book book;

    /** Day the book was borrowed. */
    @DatabaseField(canBeNull = false, persisterClass = LocalDatePersister.class)
    private LocalDate loanDate;

    /** Day the book must be returned by. */
    @DatabaseField(canBeNull = false, persisterClass = LocalDatePersister.class)
    private LocalDate dueDate;

    /** Day the book was returned, or {@code null} while the loan is open. */
    @DatabaseField(persisterClass = LocalDatePersister.class)
    private LocalDate returnDate;

    /** Whether the book has been returned. */
    @DatabaseField
    private boolean returned;

    /** Fee charged for returning the book late. */
    @DatabaseField
    private double overdueFee;

    /** Creates an empty loan. ORMLite uses it to build loans read from the database. */
    public Loan() {
    }

    /**
     * Creates an open loan.
     *
     * @param member   member who borrows the book
     * @param book     book that is borrowed
     * @param loanDate day the book is borrowed
     * @param dueDate  day the book must be returned by
     */
    public Loan(Member member, Book book, LocalDate loanDate, LocalDate dueDate) {
        this.member = member;
        this.book = book;
        this.loanDate = loanDate;
        this.dueDate = dueDate;
        this.returned = false;
        this.overdueFee = 0.0;
    }

    /**
     * Returns the identifier of the loan.
     *
     * @return the identifier
     */
    public int getId() {
        return id;
    }

    /**
     * Sets the identifier of the loan.
     *
     * @param id the identifier
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * Returns the member who borrowed the book.
     *
     * @return the member
     */
    public Member getMember() {
        return member;
    }

    /**
     * Sets the member who borrowed the book.
     *
     * @param member the member
     */
    public void setMember(Member member) {
        this.member = member;
    }

    /**
     * Returns the borrowed book.
     *
     * @return the book
     */
    public Book getBook() {
        return book;
    }

    /**
     * Sets the borrowed book.
     *
     * @param book the book
     */
    public void setBook(Book book) {
        this.book = book;
    }

    /**
     * Returns the day the book was borrowed.
     *
     * @return the loan date
     */
    public LocalDate getLoanDate() {
        return loanDate;
    }

    /**
     * Sets the day the book was borrowed.
     *
     * @param loanDate the loan date
     */
    public void setLoanDate(LocalDate loanDate) {
        this.loanDate = loanDate;
    }

    /**
     * Returns the day the book must be returned by.
     *
     * @return the due date
     */
    public LocalDate getDueDate() {
        return dueDate;
    }

    /**
     * Sets the day the book must be returned by.
     *
     * @param dueDate the due date
     */
    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    /**
     * Returns the day the book was returned.
     *
     * @return the return date, or {@code null} while the loan is open
     */
    public LocalDate getReturnDate() {
        return returnDate;
    }

    /**
     * Sets the day the book was returned.
     *
     * @param returnDate the return date
     */
    public void setReturnDate(LocalDate returnDate) {
        this.returnDate = returnDate;
    }

    /**
     * Tells whether the book has been returned.
     *
     * @return {@code true} if the loan is closed
     */
    public boolean isReturned() {
        return returned;
    }

    /**
     * Marks the loan as returned or open.
     *
     * @param returned {@code true} if the book has been returned
     */
    public void setReturned(boolean returned) {
        this.returned = returned;
    }

    /**
     * Returns the fee charged for a late return.
     *
     * @return the fee
     */
    public double getOverdueFee() {
        return overdueFee;
    }

    /**
     * Sets the fee charged for a late return.
     *
     * @param overdueFee the fee
     */
    public void setOverdueFee(double overdueFee) {
        this.overdueFee = overdueFee;
    }
}
