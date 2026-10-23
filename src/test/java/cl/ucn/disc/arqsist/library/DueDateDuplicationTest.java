/*
 * Copyright (c) 2026. Arquitectura de Sistemas, DISC, UCN, Antofagasta.
 */
package cl.ucn.disc.arqsist.library;

import cl.ucn.disc.arqsist.library.dao.*;
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
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Compares loan periods while reservation clock injection remains a later increment. */
class DueDateDuplicationTest {

    /** Database shared by the services. */
    private Database database;
    /** Clock-based checkout service. */
    private LoanService loanService;
    /** Existing reservation service, pending Change 9. */
    private ReservationService reservationService;
    /** Book with capacity for both lending paths. */
    private Book book;
    /** Borrowing member. */
    private Member member;

    /**
     * Creates both services with their currently supported constructors.
     *
     * @throws Exception if setup fails
     */
    @BeforeEach
    void setUp() throws Exception {
        database = new Database("jdbc:sqlite::memory:");
        BookDao bookDao = new OrmLiteBookDao(database.connectionSource());
        MemberDao memberDao = new OrmLiteMemberDao(database.connectionSource());
        LoanDao loanDao = new OrmLiteLoanDao(database.connectionSource());
        ReservationDao reservationDao = new OrmLiteReservationDao(database.connectionSource());
        Clock clock = Clock.fixed(Instant.parse("2026-01-15T10:00:00Z"), ZoneOffset.UTC);
        loanService = new LoanService(loanDao, memberDao, new BookService(bookDao), clock);
        reservationService = new ReservationService(reservationDao, bookDao, memberDao, loanDao);
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
     * Both lending paths use the policy's period despite their temporarily different clocks.
     * Absolute due-date equality will be checked once Change 9 injects the same fixed clock.
     *
     * @throws Exception if checkout or reservation fulfillment fails
     */
    @Test
    void checkoutAndFulfillUseTheSameLoanPeriod() throws Exception {
        Loan fromCheckout = loanService.checkout(member.getId(), book.getId());
        Reservation reservation = reservationService.reserve(book.getId(), member.getId());
        Loan fromFulfill = reservationService.fulfill(reservation.getId());
        assertEquals(LoanPolicy.DUE_DAYS,
                ChronoUnit.DAYS.between(fromCheckout.getLoanDate(), fromCheckout.getDueDate()));
        assertEquals(LoanPolicy.DUE_DAYS,
                ChronoUnit.DAYS.between(fromFulfill.getLoanDate(), fromFulfill.getDueDate()));
    }
}
