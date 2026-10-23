/*
 * Copyright (c) 2026. Arquitectura de Sistemas, DISC, UCN, Antofagasta.
 */
package cl.ucn.disc.arqsist.library.service;

import cl.ucn.disc.arqsist.library.dao.LoanDao;
import cl.ucn.disc.arqsist.library.dao.MemberDao;
import cl.ucn.disc.arqsist.library.model.Book;
import cl.ucn.disc.arqsist.library.model.Loan;
import cl.ucn.disc.arqsist.library.model.Member;

import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/** Owns lending operations, transactional inventory changes, and clock-based late fees. */
public final class LoanService {

    /** DAO used to read and persist loans. */
    private final LoanDao loanDao;

    /** DAO used to validate members before lending. */
    private final MemberDao memberDao;

    /** Catalog service that owns borrowing and returning inventory operations. */
    private final BookService bookService;

    /** Clock used for all dates calculated by this service. */
    private final Clock clock;

    /**
     * Creates a loan service.
     *
     * @param loanDao DAO used to read and persist loans
     * @param memberDao DAO used to look up members
     * @param bookService service used to look up books and change inventory
     * @param clock clock that supplies the current date
     */
    public LoanService(LoanDao loanDao, MemberDao memberDao, BookService bookService, Clock clock) {
        this.loanDao = loanDao;
        this.memberDao = memberDao;
        this.bookService = bookService;
        this.clock = clock;
    }

    /**
     * Lists all loans with current fees on open overdue loans. Calculated fees are not saved.
     *
     * @return all loans
     * @throws RuntimeException if the loans cannot be read
     */
    public List<Loan> findAll() {
        return withAccruedFees(loanDao.findAll());
    }

    /**
     * Borrows a book and creates its loan in one transaction. All injected persistence
     * collaborators must use the same database connection source.
     *
     * @param memberId identifier of the borrowing member
     * @param bookId identifier of the book to borrow
     * @return the persisted loan with its policy-based due date
     * @throws NotFoundException if the member or book does not exist
     * @throws IllegalStateException if no copy is available
     * @throws SQLException if the transaction fails for a database reason
     * @throws RuntimeException if a persistence operation fails
     */
    public Loan checkout(int memberId, int bookId) throws SQLException {
        return loanDao.transaction(() -> {
            Member member = memberDao.findById(memberId);
            if (member == null) {
                throw new NotFoundException("Member not found: " + memberId);
            }
            Book book = bookService.findById(bookId);
            if (book == null) {
                throw new NotFoundException("Book not found: " + bookId);
            }

            LocalDate today = LocalDate.now(clock);
            bookService.borrow(bookId);
            Loan loan = new Loan(member, bookService.findById(bookId), today, LoanPolicy.dueDate(today));
            loanDao.create(loan);
            return loan;
        });
    }

    /**
     * Returns a borrowed book. The loan is closed, a late fee is set when the loan is past its
     * due date, and the copy goes back to the book.
     *
     * @param loanId identifier of the loan to close
     * @return the closed loan with its final fee and return date
     * @throws NotFoundException if the loan or its book does not exist
     * @throws IllegalStateException if the loan was already returned
     * @throws SQLException if the transaction fails for a database reason
     * @throws RuntimeException if a persistence operation fails
     */
    public Loan returnLoan(int loanId) throws SQLException {
        return loanDao.transaction(() -> {
            Loan loan = loanDao.findById(loanId);
            if (loan == null) {
                throw new NotFoundException("Loan not found: " + loanId);
            }
            if (loan.isReturned()) {
                throw new IllegalStateException("Loan already returned: " + loanId);
            }

            LocalDate today = LocalDate.now(clock);
            loan.setReturned(true);
            loan.setReturnDate(today);
            if (today.isAfter(loan.getDueDate())) {
                loan.setOverdueFee(accruedFee(loan, today));
            }
            loanDao.update(loan);
            int bookId = loan.getBook().getId();
            bookService.returnCopy(bookId);
            loan.setBook(bookService.findById(bookId));
            return loan;
        });
    }

    /**
     * Lists the open loans whose due date is before today.
     *
     * @return the overdue loans
     * @throws RuntimeException if the loans cannot be read
     */
    public List<Loan> overdueLoans() {
        LocalDate today = LocalDate.now(clock);
        return loanDao.findAll().stream()
                .filter(loan -> !loan.isReturned() && loan.getDueDate().isBefore(today))
                .toList();
    }

    /**
     * Adds current fees to open overdue loan objects without updating the database.
     *
     * @param loans loans read from persistence
     * @return the same list with accrued fees applied in memory
     */
    private List<Loan> withAccruedFees(List<Loan> loans) {
        LocalDate today = LocalDate.now(clock);
        for (Loan loan : loans) {
            if (!loan.isReturned() && loan.getDueDate().isBefore(today)) {
                loan.setOverdueFee(accruedFee(loan, today));
            }
        }
        return loans;
    }

    /**
     * Calculates the fee for a loan known to be past due.
     *
     * @param loan overdue loan
     * @param today date used to count overdue days
     * @return overdue days multiplied by the policy's daily rate
     */
    private double accruedFee(Loan loan, LocalDate today) {
        return ChronoUnit.DAYS.between(loan.getDueDate(), today) * LoanPolicy.FEE_PER_DAY;
    }
}
