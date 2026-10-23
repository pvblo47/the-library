/*
 * Copyright (c) 2026. Arquitectura de Sistemas, DISC, UCN, Antofagasta.
 */
package cl.ucn.disc.arqsist.library.service;

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
import com.j256.ormlite.dao.DaoManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/** Tests reservation validation, clock-based dates, inventory, and multi-write rollback. */
class ReservationServiceTest {

    /** Deterministic date used by the default service. */
    private static final LocalDate TODAY = LocalDate.of(2026, 1, 15);
    /** Clock shared by reservation creation and fulfillment. */
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-01-15T10:00:00Z"), ZoneOffset.UTC);
    /** Fresh in-memory database for every test. */
    private Database database;
    /** Book persistence used to verify available copies. */
    private BookDao bookDao;
    /** Member persistence supplied to the service. */
    private MemberDao memberDao;
    /** Loan persistence used to verify fulfillment results. */
    private LoanDao loanDao;
    /** Reservation persistence used to verify stored flags and dates. */
    private ReservationDao reservationDao;
    /** Service under test. */
    private ReservationService service;
    /** Persisted book initially containing two available copies. */
    private Book book;
    /** Persisted reserving member. */
    private Member member;

    /**
     * Builds all collaborators against a single in-memory database connection source.
     *
     * @throws Exception if database setup fails
     */
    @BeforeEach
    void setUp() throws Exception {
        database = new Database("jdbc:sqlite::memory:");
        bookDao = new OrmLiteBookDao(database.connectionSource());
        memberDao = new OrmLiteMemberDao(database.connectionSource());
        loanDao = new OrmLiteLoanDao(database.connectionSource());
        reservationDao = new OrmLiteReservationDao(database.connectionSource());
        service = new ReservationService(reservationDao, memberDao, loanDao, new BookService(bookDao), CLOCK);
        book = new Book("Refactoring", "Martin Fowler", "9780134757599", 2);
        bookDao.create(book);
        member = new Member("Grace Hopper", "grace@example.com");
        memberDao.create(member);
    }

    /**
     * Closes the test database.
     *
     * @throws Exception if closing the connection fails
     */
    @AfterEach
    void tearDown() throws Exception {
        database.connectionSource().close();
    }

    /** Creating a reservation stores the fixed date without consuming a copy or creating a loan. */
    @Test
    void reservePersistsClockDateWithoutBorrowing() {
        Reservation reservation = service.reserve(book.getId(), member.getId());
        Reservation stored = reservationDao.findById(reservation.getId());
        assertEquals(TODAY, stored.getReservedAt());
        assertEquals(member.getId(), stored.getMember().getId());
        assertEquals(book.getId(), stored.getBook().getId());
        assertFalse(stored.isFulfilled());
        assertEquals(2, bookDao.findById(book.getId()).getAvailableCopies());
        assertTrue(loanDao.findAll().isEmpty());
        assertEquals(1, service.findAll().size());
        assertEquals(reservation.getId(), service.findAll().getFirst().getId());
    }

    /** Missing books are rejected before any reservation is written. */
    @Test
    void reserveRejectsMissingBook() {
        NotFoundException error = assertThrows(NotFoundException.class,
                () -> service.reserve(9999, member.getId()));
        assertEquals("Book not found: 9999", error.getMessage());
        assertTrue(reservationDao.findAll().isEmpty());
        assertTrue(loanDao.findAll().isEmpty());
        assertEquals(2, bookDao.findById(book.getId()).getAvailableCopies());
    }

    /** Missing members are rejected before any reservation is written. */
    @Test
    void reserveRejectsMissingMember() {
        NotFoundException error = assertThrows(NotFoundException.class,
                () -> service.reserve(book.getId(), 9999));
        assertEquals("Member not found: 9999", error.getMessage());
        assertTrue(reservationDao.findAll().isEmpty());
        assertTrue(loanDao.findAll().isEmpty());
        assertEquals(2, bookDao.findById(book.getId()).getAvailableCopies());
    }

    /**
     * Successful fulfillment stores the reservation flag and loan while consuming one copy.
     *
     * @throws Exception if fulfillment fails
     */
    @Test
    void fulfillPersistsReservationLoanAndInventory() throws Exception {
        Reservation reservation = service.reserve(book.getId(), member.getId());
        Loan loan = service.fulfill(reservation.getId());
        Loan stored = loanDao.findById(loan.getId());
        assertTrue(reservationDao.findById(reservation.getId()).isFulfilled());
        assertEquals(TODAY, reservationDao.findById(reservation.getId()).getReservedAt());
        assertEquals(TODAY, stored.getLoanDate());
        assertEquals(LocalDate.of(2026, 2, 5), stored.getDueDate());
        assertEquals(member.getId(), stored.getMember().getId());
        assertEquals(book.getId(), stored.getBook().getId());
        assertFalse(stored.isReturned());
        assertNull(stored.getReturnDate());
        assertEquals(0.0, stored.getOverdueFee());
        assertEquals(1, bookDao.findById(book.getId()).getAvailableCopies());
        assertEquals(1, loan.getBook().getAvailableCopies());
        assertEquals(1, loanDao.findAll().size());
    }

    /** A missing reservation produces a domain error without any write. */
    @Test
    void fulfillRejectsMissingReservation() {
        NotFoundException error = assertThrows(NotFoundException.class, () -> service.fulfill(9999));
        assertEquals("Reservation not found: 9999", error.getMessage());
        assertTrue(reservationDao.findAll().isEmpty());
        assertTrue(loanDao.findAll().isEmpty());
        assertEquals(2, bookDao.findById(book.getId()).getAvailableCopies());
    }

    /**
     * Repeated fulfillment never consumes another copy or creates another loan.
     *
     * @throws Exception if the initial fulfillment fails
     */
    @Test
    void fulfillRejectsAlreadyFulfilledReservation() throws Exception {
        Reservation reservation = service.reserve(book.getId(), member.getId());
        Loan loan = service.fulfill(reservation.getId());
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> service.fulfill(reservation.getId()));
        assertEquals("Reservation already fulfilled: " + reservation.getId(), error.getMessage());
        assertTrue(reservationDao.findById(reservation.getId()).isFulfilled());
        assertEquals(1, loanDao.findAll().size());
        assertEquals(loan.getId(), loanDao.findAll().getFirst().getId());
        assertEquals(1, bookDao.findById(book.getId()).getAvailableCopies());
    }

    /** An availability failure rolls back the reservation update performed before borrowing. */
    @Test
    void fulfillRejectsZeroCopiesAndKeepsReservationPending() {
        book.setAvailableCopies(0);
        bookDao.update(book);
        Reservation reservation = service.reserve(book.getId(), member.getId());
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> service.fulfill(reservation.getId()));
        assertEquals("No available copies of book " + book.getId(), error.getMessage());
        assertPendingState(reservation.getId(), 0);
    }

    /** Invalid negative inventory is rejected and never decremented further. */
    @Test
    void fulfillRejectsNegativeCopiesAndKeepsReservationPending() {
        book.setAvailableCopies(-1);
        bookDao.update(book);
        Reservation reservation = service.reserve(book.getId(), member.getId());
        assertThrows(IllegalStateException.class, () -> service.fulfill(reservation.getId()));
        assertPendingState(reservation.getId(), -1);
    }

    /**
     * A reservation-update failure leaves the flag, inventory, and loans unchanged.
     *
     * @throws Exception if trigger setup or recovery fails
     */
    @Test
    void fulfillRollsBackWhenReservationUpdateFails() throws Exception {
        Reservation reservation = service.reserve(book.getId(), member.getId());
        executeSql("CREATE TRIGGER fail_reservation_update AFTER UPDATE ON reservations "
                + "BEGIN SELECT RAISE(ABORT, 'injected reservation update failure'); END");
        RuntimeException error = assertThrows(RuntimeException.class, () -> service.fulfill(reservation.getId()));
        assertInjectedFailure(error, "injected reservation update failure");
        assertPendingState(reservation.getId(), 2);
        executeSql("DROP TRIGGER fail_reservation_update");
        assertRetrySucceeds(reservation.getId());
    }

    /**
     * An inventory-update failure also rolls back the earlier fulfilled flag.
     *
     * @throws Exception if trigger setup or recovery fails
     */
    @Test
    void fulfillRollsBackWhenInventoryUpdateFails() throws Exception {
        Reservation reservation = service.reserve(book.getId(), member.getId());
        executeSql("CREATE TRIGGER fail_reservation_book_update AFTER UPDATE ON books "
                + "BEGIN SELECT RAISE(ABORT, 'injected reservation inventory failure'); END");
        RuntimeException error = assertThrows(RuntimeException.class, () -> service.fulfill(reservation.getId()));
        assertInjectedFailure(error, "injected reservation inventory failure");
        assertPendingState(reservation.getId(), 2);
        executeSql("DROP TRIGGER fail_reservation_book_update");
        assertRetrySucceeds(reservation.getId());
    }

    /**
     * A late loan-insert failure rolls back both the reservation update and inventory decrement.
     *
     * @throws Exception if trigger setup or recovery fails
     */
    @Test
    void fulfillRollsBackReservationAndInventoryWhenLoanInsertFails() throws Exception {
        Reservation reservation = service.reserve(book.getId(), member.getId());
        executeSql("CREATE TRIGGER fail_reservation_loan_insert AFTER INSERT ON loans "
                + "BEGIN SELECT RAISE(ABORT, 'injected reservation loan failure'); END");
        RuntimeException error = assertThrows(RuntimeException.class, () -> service.fulfill(reservation.getId()));
        assertInjectedFailure(error, "injected reservation loan failure");
        assertPendingState(reservation.getId(), 2);
        executeSql("DROP TRIGGER fail_reservation_loan_insert");
        assertRetrySucceeds(reservation.getId());
    }

    /**
     * Reservation and loan dates honor the injected zone at a UTC date boundary.
     *
     * @throws Exception if fulfillment fails
     */
    @Test
    void reserveAndFulfillUseClockTimeZone() throws Exception {
        Clock santiago = Clock.fixed(Instant.parse("2026-01-15T01:00:00Z"), ZoneId.of("America/Santiago"));
        ReservationService localService = new ReservationService(reservationDao, memberDao, loanDao,
                new BookService(bookDao), santiago);
        Reservation reservation = localService.reserve(book.getId(), member.getId());
        Loan loan = localService.fulfill(reservation.getId());
        assertEquals(LocalDate.of(2026, 1, 14), reservationDao.findById(reservation.getId()).getReservedAt());
        assertEquals(LocalDate.of(2026, 1, 14), loanDao.findById(loan.getId()).getLoanDate());
        assertEquals(LocalDate.of(2026, 2, 4), loanDao.findById(loan.getId()).getDueDate());
    }

    /**
     * Checks all persisted state after a failed fulfillment.
     *
     * @param reservationId reservation expected to remain pending
     * @param expectedCopies inventory expected after rollback
     */
    private void assertPendingState(int reservationId, int expectedCopies) {
        assertEquals(1, reservationDao.findAll().size());
        assertFalse(reservationDao.findById(reservationId).isFulfilled());
        assertEquals(TODAY, reservationDao.findById(reservationId).getReservedAt());
        assertTrue(loanDao.findAll().isEmpty());
        assertEquals(expectedCopies, bookDao.findById(book.getId()).getAvailableCopies());
    }

    /**
     * Verifies a failed transaction leaves the database usable for a successful retry.
     *
     * @param reservationId reservation to fulfill after removing the failure trigger
     * @throws Exception if retrying fulfillment fails
     */
    private void assertRetrySucceeds(int reservationId) throws Exception {
        Loan loan = service.fulfill(reservationId);
        assertTrue(reservationDao.findById(reservationId).isFulfilled());
        assertEquals(1, loanDao.findAll().size());
        assertEquals(loan.getId(), loanDao.findAll().getFirst().getId());
        assertEquals(1, bookDao.findById(book.getId()).getAvailableCopies());
    }

    /**
     * Ensures a failure came from the intended trigger rather than unrelated service logic.
     *
     * @param failure exception returned by the service
     * @param expectedMessage identifying text raised by the trigger
     */
    private void assertInjectedFailure(RuntimeException failure, String expectedMessage) {
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            if (cause.getMessage() != null && cause.getMessage().contains(expectedMessage)) {
                return;
            }
        }
        fail("Expected the injected database failure: " + expectedMessage, failure);
    }

    /**
     * Installs or removes a failure trigger in the test database.
     *
     * @param sql SQLite trigger statement
     * @throws Exception if the statement fails
     */
    private void executeSql(String sql) throws Exception {
        DaoManager.createDao(database.connectionSource(), Reservation.class).executeRaw(sql);
    }
}
