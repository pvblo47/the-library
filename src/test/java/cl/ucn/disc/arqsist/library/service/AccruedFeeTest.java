/*
 * Copyright (c) 2026. Arquitectura de Sistemas, DISC, UCN, Antofagasta.
 */
package cl.ucn.disc.arqsist.library.service;

import cl.ucn.disc.arqsist.library.dao.*;
import cl.ucn.disc.arqsist.library.db.Database;
import cl.ucn.disc.arqsist.library.model.Book;
import cl.ucn.disc.arqsist.library.model.Loan;
import cl.ucn.disc.arqsist.library.model.Member;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Verifies accrued fees are calculated on reads and never written back by listing loans. */
class AccruedFeeTest {

    /**
     * Both listings accrue the same fee without persisting it. Only open overdue loans appear
     * in the overdue listing; active, due-today, and returned loans are excluded.
     *
     * @throws Exception if database setup or closing fails
     */
    @Test
    void bothListingsAccrueFeesWithoutPersistingThem() throws Exception {
        Database database = new Database("jdbc:sqlite::memory:");
        try {
            BookDao bookDao = new OrmLiteBookDao(database.connectionSource());
            MemberDao memberDao = new OrmLiteMemberDao(database.connectionSource());
            LoanDao loanDao = new OrmLiteLoanDao(database.connectionSource());
            Clock clock = Clock.fixed(Instant.parse("2026-01-15T10:00:00Z"), ZoneOffset.UTC);
            LoanService service = new LoanService(loanDao, memberDao, new BookService(bookDao), clock);
            Book book = new Book("Refactoring", "Martin Fowler", "9780134757599", 4);
            Member member = new Member("Ada Lovelace", "ada@example.com");
            bookDao.create(book);
            memberDao.create(member);
            LocalDate today = LocalDate.of(2026, 1, 15);
            Loan overdue = new Loan(member, book, today.minusDays(45), LocalDate.of(2025, 12, 22));
            Loan active = new Loan(member, book, today, today.plusDays(21));
            Loan dueToday = new Loan(member, book, today.minusDays(21), today);
            Loan returned = new Loan(member, book, today.minusDays(40), today.minusDays(19));
            returned.setReturned(true);
            returned.setReturnDate(today.minusDays(12));
            returned.setOverdueFee(7.0);
            loanDao.create(overdue);
            loanDao.create(active);
            loanDao.create(dueToday);
            loanDao.create(returned);

            List<Loan> overdueListed = service.overdueLoans();
            assertEquals(1, overdueListed.size());
            assertEquals(overdue.getId(), overdueListed.getFirst().getId());
            assertEquals(24.0, feeOf(overdueListed, overdue.getId()));
            assertEquals(0.0, loanDao.findById(overdue.getId()).getOverdueFee());

            List<Loan> listed = service.findAll();
            assertEquals(24.0, feeOf(listed, overdue.getId()));
            assertEquals(feeOf(listed, overdue.getId()), feeOf(overdueListed, overdue.getId()));
            assertEquals(0.0, feeOf(listed, active.getId()));
            assertEquals(0.0, feeOf(listed, dueToday.getId()));
            assertEquals(7.0, feeOf(listed, returned.getId()));
            assertEquals(0.0, loanDao.findById(overdue.getId()).getOverdueFee());
            assertEquals(0.0, loanDao.findById(active.getId()).getOverdueFee());
            assertEquals(0.0, loanDao.findById(dueToday.getId()).getOverdueFee());
            assertEquals(7.0, loanDao.findById(returned.getId()).getOverdueFee());
            assertEquals(24.0, feeOf(service.findAll(), overdue.getId()));
            assertEquals(24.0, feeOf(service.overdueLoans(), overdue.getId()));
            assertEquals(0.0, loanDao.findById(overdue.getId()).getOverdueFee());
        } finally {
            database.connectionSource().close();
        }
    }

    /**
     * Looks up a loan's displayed fee without relying on database row order.
     *
     * @param loans loans returned by the service
     * @param id identifier of the desired loan
     * @return the displayed fee
     * @throws java.util.NoSuchElementException if the loan is absent
     */
    private double feeOf(List<Loan> loans, int id) {
        return loans.stream().filter(loan -> loan.getId() == id).findFirst().orElseThrow().getOverdueFee();
    }
}
