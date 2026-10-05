/*
 * Copyright (c) 2026. Arquitectura de Sistemas, DISC, UCN, Antofagasta.
 */
package cl.ucn.disc.arqsist.library.service;

import cl.ucn.disc.arqsist.library.dao.BookDao;
import cl.ucn.disc.arqsist.library.dao.LoanDao;
import cl.ucn.disc.arqsist.library.dao.MemberDao;
import cl.ucn.disc.arqsist.library.dao.OrmLiteBookDao;
import cl.ucn.disc.arqsist.library.dao.OrmLiteLoanDao;
import cl.ucn.disc.arqsist.library.dao.OrmLiteMemberDao;
import cl.ucn.disc.arqsist.library.db.Database;
import cl.ucn.disc.arqsist.library.model.Book;
import cl.ucn.disc.arqsist.library.model.Loan;
import cl.ucn.disc.arqsist.library.model.Member;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Tests for {@link LoanService#overdueLoans()} after the dates became {@link LocalDate}. */
class LoanServiceTest {

    /** In-memory database used by each test. */
    private Database database;

    /** DAO used to create the test loans. */
    private LoanDao loanDao;

    /** Service under test. */
    private LoanService service;

    /** A persisted book that the test loans refer to. */
    private Book book;

    /** A persisted member that the test loans refer to. */
    private Member member;

    /**
     * Creates a fresh in-memory database, the service, one book and one member.
     *
     * @throws Exception if the database cannot be created
     */
    @BeforeEach
    void setUp() throws Exception {
        database = new Database("jdbc:sqlite::memory:");
        BookDao bookDao = new OrmLiteBookDao(database.connectionSource());
        MemberDao memberDao = new OrmLiteMemberDao(database.connectionSource());
        loanDao = new OrmLiteLoanDao(database.connectionSource());
        service = new LoanService(loanDao, bookDao);
        book = new Book("Clean Code", "Robert C. Martin", "9780132350884", 3);
        bookDao.create(book);
        member = new Member("Ada Lovelace", "ada@example.com");
        memberDao.create(member);
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

    /** Only open loans whose due date is before today are overdue. */
    @Test
    void overdueLoansListsOnlyOpenLoansPastTheirDueDate() {
        LocalDate today = LocalDate.now();
        Loan overdue = new Loan(member, book, today.minusDays(30), today.minusDays(9));
        Loan active = new Loan(member, book, today.minusDays(2), today.plusDays(19));
        Loan returned = new Loan(member, book, today.minusDays(40), today.minusDays(19));
        returned.setReturned(true);
        returned.setReturnDate(today.minusDays(20));
        loanDao.create(overdue);
        loanDao.create(active);
        loanDao.create(returned);

        List<Loan> result = service.overdueLoans();

        assertEquals(1, result.size());
        assertEquals(overdue.getId(), result.get(0).getId());
    }
}
