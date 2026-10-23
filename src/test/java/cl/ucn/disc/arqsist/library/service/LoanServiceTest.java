/*
 * Copyright (c) 2026. Arquitectura de Sistemas, DISC, UCN, Antofagasta.
 */
package cl.ucn.disc.arqsist.library.service;

import cl.ucn.disc.arqsist.library.dao.*;
import cl.ucn.disc.arqsist.library.db.Database;
import cl.ucn.disc.arqsist.library.model.Book;
import cl.ucn.disc.arqsist.library.model.Loan;
import cl.ucn.disc.arqsist.library.model.Member;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Tests lending, return guards, inventory persistence, and deterministic service dates. */
class LoanServiceTest {

    /** Fixed date shared by the tests. */
    private static final LocalDate TODAY = LocalDate.of(2026, 1, 15);
    /** Clock independent of the machine's date and time zone. */
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-01-15T10:00:00Z"), ZoneOffset.UTC);
    /** Fresh database for each test. */
    private Database database;
    /** Book persistence used to verify inventory. */
    private BookDao bookDao;
    /** Member persistence used by the service. */
    private MemberDao memberDao;
    /** Loan persistence used to verify stored state. */
    private LoanDao loanDao;
    /** Service under test. */
    private LoanService service;
    /** Persisted book with three copies. */
    private Book book;
    /** Persisted borrowing member. */
    private Member member;

    /**
     * Creates the in-memory database and service collaborators.
     *
     * @throws Exception if database setup fails
     */
    @BeforeEach
    void setUp() throws Exception {
        database = new Database("jdbc:sqlite::memory:");
        bookDao = new OrmLiteBookDao(database.connectionSource());
        memberDao = new OrmLiteMemberDao(database.connectionSource());
        loanDao = new OrmLiteLoanDao(database.connectionSource());
        service = new LoanService(loanDao, memberDao, new BookService(bookDao), CLOCK);
        book = new Book("Clean Code", "Robert C. Martin", "9780132350884", 3);
        bookDao.create(book);
        member = new Member("Ada Lovelace", "ada@example.com");
        memberDao.create(member);
    }

    /**
     * Releases the test database.
     *
     * @throws Exception if the connection cannot be closed
     */
    @AfterEach
    void tearDown() throws Exception {
        database.connectionSource().close();
    }

    /**
     * Checkout persists the policy dates and consumes exactly one copy.
     *
     * @throws Exception if checkout fails
     */
    @Test
    void checkoutPersistsLoanAndInventory() throws Exception {
        Loan loan = service.checkout(member.getId(), book.getId());
        Loan stored = loanDao.findById(loan.getId());
        assertEquals(TODAY, stored.getLoanDate());
        assertEquals(LocalDate.of(2026, 2, 5), stored.getDueDate());
        assertEquals(member.getId(), stored.getMember().getId());
        assertFalse(stored.isReturned());
        assertNull(stored.getReturnDate());
        assertEquals(0.0, stored.getOverdueFee());
        assertEquals(2, bookDao.findById(book.getId()).getAvailableCopies());
        assertEquals(2, loan.getBook().getAvailableCopies());
    }

    /** Missing members leave both loans and inventory untouched. */
    @Test
    void checkoutRejectsMissingMember() {
        NotFoundException error = assertThrows(NotFoundException.class,
                () -> service.checkout(9999, book.getId()));
        assertEquals("Member not found: 9999", error.getMessage());
        assertEquals(3, bookDao.findById(book.getId()).getAvailableCopies());
        assertTrue(loanDao.findAll().isEmpty());
    }

    /** Missing books are reported as domain errors without creating loans. */
    @Test
    void checkoutRejectsMissingBook() {
        NotFoundException error = assertThrows(NotFoundException.class,
                () -> service.checkout(member.getId(), 9999));
        assertEquals("Book not found: 9999", error.getMessage());
        assertTrue(loanDao.findAll().isEmpty());
    }

    /** Exhausted inventory is rejected without a negative count or a new loan. */
    @Test
    void checkoutRejectsZeroCopies() {
        book.setAvailableCopies(0);
        bookDao.update(book);
        assertThrows(IllegalStateException.class, () -> service.checkout(member.getId(), book.getId()));
        assertEquals(0, bookDao.findById(book.getId()).getAvailableCopies());
        assertTrue(loanDao.findAll().isEmpty());
    }

    /** Previously invalid negative inventory is rejected without decrementing it again. */
    @Test
    void checkoutRejectsNegativeCopies() {
        book.setAvailableCopies(-1);
        bookDao.update(book);
        assertThrows(IllegalStateException.class, () -> service.checkout(member.getId(), book.getId()));
        assertEquals(-1, bookDao.findById(book.getId()).getAvailableCopies());
        assertTrue(loanDao.findAll().isEmpty());
    }

    /**
     * An early return closes the loan and restores one copy without a fee.
     *
     * @throws Exception if checkout or return fails
     */
    @Test
    void returnPersistsLoanAndRestoresCopy() throws Exception {
        Loan loan = service.checkout(member.getId(), book.getId());
        Loan returned = service.returnLoan(loan.getId());
        Loan stored = loanDao.findById(loan.getId());
        assertTrue(stored.isReturned());
        assertEquals(TODAY, stored.getReturnDate());
        assertEquals(0.0, stored.getOverdueFee());
        assertEquals(3, bookDao.findById(book.getId()).getAvailableCopies());
        assertEquals(3, returned.getBook().getAvailableCopies());
    }

    /**
     * Returning on the due date does not charge a late fee.
     *
     * @throws Exception if checkout or return fails
     */
    @Test
    void returnOnDueDateHasNoFee() throws Exception {
        Loan loan = service.checkout(member.getId(), book.getId());
        loan.setDueDate(TODAY);
        loanDao.update(loan);
        service.returnLoan(loan.getId());
        assertEquals(0.0, loanDao.findById(loan.getId()).getOverdueFee());
    }

    /**
     * A late return stores the fee for the elapsed overdue days.
     *
     * @throws Exception if checkout or return fails
     */
    @Test
    void lateReturnPersistsFinalFee() throws Exception {
        Loan loan = service.checkout(member.getId(), book.getId());
        loan.setDueDate(LocalDate.of(2025, 12, 22));
        loanDao.update(loan);
        service.returnLoan(loan.getId());
        assertEquals(24.0, loanDao.findById(loan.getId()).getOverdueFee());
        assertEquals(3, bookDao.findById(book.getId()).getAvailableCopies());
    }

    /** A missing loan is rejected without changing inventory. */
    @Test
    void returnRejectsMissingLoan() {
        NotFoundException error = assertThrows(NotFoundException.class, () -> service.returnLoan(9999));
        assertEquals("Loan not found: 9999", error.getMessage());
        assertEquals(3, bookDao.findById(book.getId()).getAvailableCopies());
    }

    /**
     * Repeated returns cannot increase inventory twice.
     *
     * @throws Exception if the first checkout or return fails
     */
    @Test
    void returnRejectsAlreadyReturnedLoan() throws Exception {
        Loan loan = service.checkout(member.getId(), book.getId());
        service.returnLoan(loan.getId());
        assertThrows(IllegalStateException.class, () -> service.returnLoan(loan.getId()));
        assertEquals(3, bookDao.findById(book.getId()).getAvailableCopies());
        assertEquals(TODAY, loanDao.findById(loan.getId()).getReturnDate());
    }

    /** Only open loans strictly before the fixed current date are overdue. */
    @Test
    void overdueLoansListsOnlyOpenLoansPastTheirDueDate() {
        Loan overdue = new Loan(member, book, TODAY.minusDays(30), TODAY.minusDays(9));
        Loan active = new Loan(member, book, TODAY.minusDays(2), TODAY.plusDays(19));
        Loan dueToday = new Loan(member, book, TODAY.minusDays(21), TODAY);
        Loan returned = new Loan(member, book, TODAY.minusDays(40), TODAY.minusDays(19));
        returned.setReturned(true);
        returned.setReturnDate(TODAY.minusDays(20));
        loanDao.create(overdue);
        loanDao.create(active);
        loanDao.create(dueToday);
        loanDao.create(returned);
        List<Loan> result = service.overdueLoans();
        assertEquals(1, result.size());
        assertEquals(overdue.getId(), result.getFirst().getId());
    }

    /**
     * Service dates honor the injected zone, including the day before UTC midnight.
     *
     * @throws Exception if checkout fails
     */
    @Test
    void checkoutUsesClockTimeZone() throws Exception {
        Clock santiago = Clock.fixed(Instant.parse("2026-01-15T01:00:00Z"), ZoneId.of("America/Santiago"));
        LoanService localService = new LoanService(loanDao, memberDao, new BookService(bookDao), santiago);
        Loan loan = localService.checkout(member.getId(), book.getId());
        assertEquals(LocalDate.of(2026, 1, 14), loan.getLoanDate());
        assertEquals(LocalDate.of(2026, 2, 4), loan.getDueDate());
    }
}
