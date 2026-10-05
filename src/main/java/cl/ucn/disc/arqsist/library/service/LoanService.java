/*
 * Copyright (c) 2026. Arquitectura de Sistemas, DISC, UCN, Antofagasta.
 */
package cl.ucn.disc.arqsist.library.service;

import cl.ucn.disc.arqsist.library.dao.BookDao;
import cl.ucn.disc.arqsist.library.dao.LoanDao;
import cl.ucn.disc.arqsist.library.model.Book;
import cl.ucn.disc.arqsist.library.model.Loan;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/** Lists loans, finds overdue loans and returns borrowed books. */
public final class LoanService {

    /** Loan period in days. It is kept until Stage 2 removes it in favor of {@link LoanPolicy#DUE_DAYS}. */
    public static final int DUE_DAYS = 21;

    /** DAO used to read and persist loans. */
    private final LoanDao loanDao;

    /** DAO used to give a copy back to the book on return. */
    private final BookDao bookDao;

    /**
     * Creates a loan service.
     *
     * @param loanDao DAO used to read and persist loans
     * @param bookDao DAO used to update the copy count of books
     */
    public LoanService(LoanDao loanDao, BookDao bookDao) {
        this.loanDao = loanDao;
        this.bookDao = bookDao;
    }

    /**
     * Lists all loans.
     *
     * @return all loans
     * @throws RuntimeException if the loans cannot be read
     */
    public List<Loan> findAll() {
        return loanDao.findAll();
    }

    /**
     * Returns a borrowed book. The loan is closed, a late fee is set when the loan is past its
     * due date, and the copy goes back to the book.
     *
     * @param loanId identifier of the loan to close
     * @return the closed loan; the loan unchanged if it was already returned; {@code null} if it does not exist
     * @throws RuntimeException if a persistence operation fails
     */
    public Loan returnLoan(int loanId) {
        Loan loan = loanDao.findById(loanId);
        if (loan == null || loan.isReturned()) {
            return loan;
        }

        LocalDate today = LocalDate.now();
        loan.setReturned(true);
        loan.setReturnDate(today);

        LocalDate due = loan.getDueDate();
        if (today.isAfter(due)) {
            long daysOverdue = ChronoUnit.DAYS.between(due, today);
            loan.setOverdueFee(daysOverdue * 1.0);
        }

        loanDao.update(loan);

        Book book = loan.getBook();
        book.setAvailableCopies(book.getAvailableCopies() + 1);
        bookDao.update(book);

        return loan;
    }

    /**
     * Lists the open loans whose due date is before today.
     *
     * @return the overdue loans
     * @throws RuntimeException if the loans cannot be read
     */
    public List<Loan> overdueLoans() {
        LocalDate today = LocalDate.now();
        return loanDao.findAll().stream()
                .filter(loan -> !loan.isReturned() && loan.getDueDate().isBefore(today))
                .toList();
    }
}
