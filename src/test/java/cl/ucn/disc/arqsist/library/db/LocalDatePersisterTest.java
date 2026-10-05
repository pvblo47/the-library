/*
 * Copyright (c) 2026. Arquitectura de Sistemas, DISC, UCN, Antofagasta.
 */
package cl.ucn.disc.arqsist.library.db;

import cl.ucn.disc.arqsist.library.dao.BookDao;
import cl.ucn.disc.arqsist.library.dao.LoanDao;
import cl.ucn.disc.arqsist.library.dao.MemberDao;
import cl.ucn.disc.arqsist.library.dao.OrmLiteBookDao;
import cl.ucn.disc.arqsist.library.dao.OrmLiteLoanDao;
import cl.ucn.disc.arqsist.library.dao.OrmLiteMemberDao;
import cl.ucn.disc.arqsist.library.dao.OrmLiteReservationDao;
import cl.ucn.disc.arqsist.library.dao.ReservationDao;
import cl.ucn.disc.arqsist.library.model.Book;
import cl.ucn.disc.arqsist.library.model.Loan;
import cl.ucn.disc.arqsist.library.model.Member;
import cl.ucn.disc.arqsist.library.model.Reservation;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** Tests that {@link LocalDatePersister} stores dates as ISO text and reads them back as {@link LocalDate}. */
class LocalDatePersisterTest {

    /** In-memory database used by each test. */
    private Database database;

    /** DAO for loans. */
    private LoanDao loanDao;

    /** DAO for reservations. */
    private ReservationDao reservationDao;

    /** A persisted book that the test loans refer to. */
    private Book book;

    /** A persisted member that the test loans refer to. */
    private Member member;

    /**
     * Creates a fresh in-memory database with one book and one member.
     *
     * @throws Exception if the database cannot be created
     */
    @BeforeEach
    void setUp() throws Exception {
        database = new Database("jdbc:sqlite::memory:");
        BookDao bookDao = new OrmLiteBookDao(database.connectionSource());
        MemberDao memberDao = new OrmLiteMemberDao(database.connectionSource());
        loanDao = new OrmLiteLoanDao(database.connectionSource());
        reservationDao = new OrmLiteReservationDao(database.connectionSource());
        book = new Book("Clean Code", "Robert C. Martin", "9780132350884", 1);
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

    /**
     * Loan dates round-trip as {@link LocalDate}, an open loan keeps a {@code null} return date,
     * and the column holds ISO text.
     *
     * @throws Exception if the raw column cannot be read
     */
    @Test
    void loanDatesRoundTripAndAreStoredAsIsoText() throws Exception {
        LocalDate loanDate = LocalDate.of(2026, 1, 15);
        LocalDate dueDate = LocalDate.of(2026, 2, 5);
        Loan loan = new Loan(member, book, loanDate, dueDate);
        loanDao.create(loan);

        Loan reloaded = loanDao.findById(loan.getId());

        assertEquals(loanDate, reloaded.getLoanDate());
        assertEquals(dueDate, reloaded.getDueDate());
        assertNull(reloaded.getReturnDate());

        Dao<Loan, Integer> rawDao = DaoManager.createDao(database.connectionSource(), Loan.class);
        String storedDueDate = rawDao.queryRaw("SELECT dueDate FROM loans").getFirstResult()[0];
        assertEquals("2026-02-05", storedDueDate);
    }

    /** A return date that is set is persisted and read back. */
    @Test
    void returnDateRoundTrips() {
        Loan loan = new Loan(member, book, LocalDate.of(2026, 1, 15), LocalDate.of(2026, 2, 5));
        loan.setReturnDate(LocalDate.of(2026, 2, 1));
        loanDao.create(loan);

        assertEquals(LocalDate.of(2026, 2, 1), loanDao.findById(loan.getId()).getReturnDate());
    }

    /** The reservation date round-trips as {@link LocalDate}. */
    @Test
    void reservationDateRoundTrips() {
        Reservation reservation = new Reservation(member, book, LocalDate.of(2026, 3, 9));
        reservationDao.create(reservation);

        assertEquals(LocalDate.of(2026, 3, 9), reservationDao.findById(reservation.getId()).getReservedAt());
    }
}
