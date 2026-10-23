/*
 * Copyright (c) 2026. Arquitectura de Sistemas, DISC, UCN, Antofagasta.
 */
package cl.ucn.disc.arqsist.library;

import cl.ucn.disc.arqsist.library.dao.*;
import cl.ucn.disc.arqsist.library.db.Database;
import cl.ucn.disc.arqsist.library.model.Book;
import cl.ucn.disc.arqsist.library.model.Loan;
import cl.ucn.disc.arqsist.library.model.Member;
import cl.ucn.disc.arqsist.library.service.BookService;
import cl.ucn.disc.arqsist.library.service.LoanService;
import cl.ucn.disc.arqsist.library.service.NotFoundException;
import com.j256.ormlite.dao.DaoManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

/** Verifies domain validation and real database rollback across lending writes. */
class TransactionBugTest {

    /** Database shared by all collaborators within a test. */
    private Database database;
    /** DAO used to inspect persisted inventory. */
    private BookDao bookDao;
    /** DAO used to inspect persisted loans. */
    private LoanDao loanDao;
    /** Lending service under test. */
    private LoanService loanService;
    /** Available test book. */
    private Book book;
    /** Valid borrower used to reach the writes before an injected failure. */
    private Member member;

    /**
     * Creates independent test data and the fixed-clock service.
     *
     * @throws Exception if setup fails
     */
    @BeforeEach
    void setUp() throws Exception {
        database = new Database("jdbc:sqlite::memory:");
        bookDao = new OrmLiteBookDao(database.connectionSource());
        MemberDao memberDao = new OrmLiteMemberDao(database.connectionSource());
        loanDao = new OrmLiteLoanDao(database.connectionSource());
        loanService = new LoanService(loanDao, memberDao, new BookService(bookDao),
                Clock.fixed(Instant.parse("2026-01-15T10:00:00Z"), ZoneOffset.UTC));
        book = new Book("Clean Code", "Robert C. Martin", "9780132350884", 2);
        bookDao.create(book);
        member = new Member("Grace Hopper", "grace@example.com");
        memberDao.create(member);
    }

    /**
     * Closes the database.
     *
     * @throws Exception if closing fails
     */
    @AfterEach
    void tearDown() throws Exception {
        database.connectionSource().close();
    }

    /** Missing-member validation occurs without a partial inventory change. */
    @Test
    void checkoutLeavesNoPartialStateOnFailure() {
        assertThrows(NotFoundException.class, () -> loanService.checkout(9999, book.getId()));
        assertEquals(2, bookDao.findById(book.getId()).getAvailableCopies());
        assertTrue(loanDao.findAll().isEmpty());
    }

    /**
     * An insert failure after borrowing restores inventory and leaves no loan.
     *
     * @throws Exception if trigger setup or recovery fails
     */
    @Test
    void checkoutRollsBackBorrowWhenLoanInsertFails() throws Exception {
        executeSql("CREATE TRIGGER fail_loan_insert AFTER INSERT ON loans "
                + "BEGIN SELECT RAISE(ABORT, 'injected loan insert failure'); END");
        assertThrows(RuntimeException.class, () -> loanService.checkout(member.getId(), book.getId()));
        assertEquals(2, bookDao.findById(book.getId()).getAvailableCopies());
        assertTrue(loanDao.findAll().isEmpty());

        executeSql("DROP TRIGGER fail_loan_insert");
        loanService.checkout(member.getId(), book.getId());
        assertEquals(1, bookDao.findById(book.getId()).getAvailableCopies());
        assertEquals(1, loanDao.findAll().size());
    }

    /**
     * A failure after updating book inventory also rolls back the earlier loan closure.
     *
     * @throws Exception if checkout, trigger setup, or recovery fails
     */
    @Test
    void returnRollsBackLoanWhenInventoryUpdateFails() throws Exception {
        Loan loan = loanService.checkout(member.getId(), book.getId());
        loan.setDueDate(loan.getLoanDate().minusDays(3));
        loanDao.update(loan);
        executeSql("CREATE TRIGGER fail_book_update AFTER UPDATE ON books "
                + "BEGIN SELECT RAISE(ABORT, 'injected inventory update failure'); END");

        assertThrows(RuntimeException.class, () -> loanService.returnLoan(loan.getId()));
        Loan stored = loanDao.findById(loan.getId());
        assertFalse(stored.isReturned());
        assertNull(stored.getReturnDate());
        assertEquals(0.0, stored.getOverdueFee());
        assertEquals(1, bookDao.findById(book.getId()).getAvailableCopies());

        executeSql("DROP TRIGGER fail_book_update");
        loanService.returnLoan(loan.getId());
        assertTrue(loanDao.findById(loan.getId()).isReturned());
        assertEquals(3.0, loanDao.findById(loan.getId()).getOverdueFee());
        assertEquals(2, bookDao.findById(book.getId()).getAvailableCopies());
    }

    /**
     * Installs or removes a SQLite failure trigger without altering production DAOs.
     *
     * @param sql trigger statement to execute
     * @throws Exception if the statement fails
     */
    private void executeSql(String sql) throws Exception {
        DaoManager.createDao(database.connectionSource(), Loan.class).executeRaw(sql);
    }
}
