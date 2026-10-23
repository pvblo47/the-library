/*
 * Copyright (c) 2026. Arquitectura de Sistemas, DISC, UCN, Antofagasta.
 */
package cl.ucn.disc.arqsist.library;

import cl.ucn.disc.arqsist.library.dao.BookDao;
import cl.ucn.disc.arqsist.library.dao.LoanDao;
import cl.ucn.disc.arqsist.library.dao.MemberDao;
import cl.ucn.disc.arqsist.library.dao.ReservationDao;
import cl.ucn.disc.arqsist.library.dao.OrmLiteBookDao;
import cl.ucn.disc.arqsist.library.dao.OrmLiteLoanDao;
import cl.ucn.disc.arqsist.library.dao.OrmLiteMemberDao;
import cl.ucn.disc.arqsist.library.dao.OrmLiteReservationDao;
import cl.ucn.disc.arqsist.library.db.Database;
import cl.ucn.disc.arqsist.library.model.Book;
import cl.ucn.disc.arqsist.library.model.Loan;
import cl.ucn.disc.arqsist.library.model.Member;
import cl.ucn.disc.arqsist.library.model.Reservation;
import cl.ucn.disc.arqsist.library.service.BookService;
import cl.ucn.disc.arqsist.library.service.LoanPolicy;
import cl.ucn.disc.arqsist.library.service.LoanService;
import cl.ucn.disc.arqsist.library.service.ReservationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Verifies both lending paths share a fixed date, the policy, and guarded inventory. */
class DueDateDuplicationTest {

    /** Database shared by the services. */
    private Database database;
    /** Book DAO used to verify persisted copy counts after each lending path. */
    private BookDao bookDao;
    /** Clock-based checkout service. */
    private LoanService loanService;
    /** Reservation service using the same clock and inventory service as checkout. */
    private ReservationService reservationService;
    /** Book with capacity for both lending paths. */
    private Book book;
    /** Borrowing member. */
    private Member member;

    /**
     * Creates both services with the same fixed clock and database connection source.
     *
     * @throws Exception if setup fails
     */
    @BeforeEach
    void setUp() throws Exception {
        database = new Database("jdbc:sqlite::memory:");
        bookDao = new OrmLiteBookDao(database.connectionSource());
        MemberDao memberDao = new OrmLiteMemberDao(database.connectionSource());
        LoanDao loanDao = new OrmLiteLoanDao(database.connectionSource());
        ReservationDao reservationDao = new OrmLiteReservationDao(database.connectionSource());
        Clock clock = Clock.fixed(Instant.parse("2026-01-15T10:00:00Z"), ZoneOffset.UTC);
        BookService bookService = new BookService(bookDao);
        loanService = new LoanService(loanDao, memberDao, bookService, clock);
        reservationService = new ReservationService(reservationDao, memberDao, loanDao, bookService, clock);
        book = new Book("Design Patterns", "Gamma et al.", "9780201633610", 2);
        bookDao.create(book);
        member = new Member("Grace Hopper", "grace@example.com");
        memberDao.create(member);
    }

    /**
     * Releases the database.
     *
     * @throws Exception if closing fails
     */
    @AfterEach
    void tearDown() throws Exception {
        database.connectionSource().close();
    }

    /**
     * Both lending paths produce identical due dates and each consumes one available copy.
     *
     * @throws Exception if checkout or reservation fulfillment fails
     */
    @Test
    void checkoutAndFulfillUseTheSameLoanPeriod() throws Exception {
        Loan fromCheckout = loanService.checkout(member.getId(), book.getId());
        assertEquals(1, bookDao.findById(book.getId()).getAvailableCopies());
        Reservation reservation = reservationService.reserve(book.getId(), member.getId());
        Loan fromFulfill = reservationService.fulfill(reservation.getId());
        assertEquals(LocalDate.of(2026, 1, 15), fromCheckout.getLoanDate());
        assertEquals(fromCheckout.getLoanDate(), fromFulfill.getLoanDate());
        assertEquals(LoanPolicy.dueDate(fromCheckout.getLoanDate()), fromCheckout.getDueDate());
        assertEquals(fromCheckout.getDueDate(), fromFulfill.getDueDate());
        assertEquals(0, bookDao.findById(book.getId()).getAvailableCopies());
    }
}
