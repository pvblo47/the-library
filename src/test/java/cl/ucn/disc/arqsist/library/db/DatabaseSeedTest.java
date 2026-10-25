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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Verifies persisted seed data, inventory accounting, and idempotence in isolated databases. */
class DatabaseSeedTest {

    /** Fresh in-memory database owned by each test. */
    private Database database;

    /** DAO used to read the persisted catalog. */
    private BookDao bookDao;

    /** DAO used to read the persisted members. */
    private MemberDao memberDao;

    /** DAO used to read the persisted loan states and dates. */
    private LoanDao loanDao;

    /** DAO used to read the persisted reservation state. */
    private ReservationDao reservationDao;

    /**
     * Creates an empty in-memory database and the existing Option 4 DAO adapters.
     *
     * @throws Exception if the database cannot be created
     */
    @BeforeEach
    void setUp() throws Exception {
        database = new Database("jdbc:sqlite::memory:");
        bookDao = new OrmLiteBookDao(database.connectionSource());
        memberDao = new OrmLiteMemberDao(database.connectionSource());
        loanDao = new OrmLiteLoanDao(database.connectionSource());
        reservationDao = new OrmLiteReservationDao(database.connectionSource());
    }

    /**
     * Closes the database connection even when an assertion fails.
     *
     * @throws Exception if closing the connection fails
     */
    @AfterEach
    void tearDown() throws Exception {
        if (database != null) {
            database.connectionSource().close();
        }
    }

    /**
     * The seed creates the required counts, relationships, and three distinct loan states.
     * Dates are classified outside the interval observed around seeding, avoiding an assumption
     * that the system date stays unchanged during the call.
     *
     * @throws Exception if seeding fails
     */
    @Test
    void seedCreatesRequiredRecordsAndLoanStates() throws Exception {
        LocalDate beforeSeed = LocalDate.now();
        database.seedIfEmpty();
        LocalDate afterSeed = LocalDate.now();

        List<Book> books = bookDao.findAll();
        List<Member> members = memberDao.findAll();
        List<Loan> loans = loanDao.findAll();
        List<Reservation> reservations = reservationDao.findAll();
        assertEquals(3, books.size());
        assertEquals(3, members.size());
        assertEquals(3, loans.size());
        assertEquals(1, reservations.size());

        assertEquals(1L, loans.stream().filter(Loan::isReturned).count());
        assertEquals(1L, loans.stream()
                .filter(loan -> !loan.isReturned() && loan.getDueDate().isBefore(beforeSeed)).count());
        assertEquals(1L, loans.stream()
                .filter(loan -> !loan.isReturned() && loan.getDueDate().isAfter(afterSeed)).count());

        Set<Integer> bookIds = books.stream().map(Book::getId).collect(Collectors.toSet());
        Set<Integer> memberIds = members.stream().map(Member::getId).collect(Collectors.toSet());
        for (Loan loan : loans) {
            assertTrue(bookIds.contains(loan.getBook().getId()));
            assertTrue(memberIds.contains(loan.getMember().getId()));
            assertFalse(loan.getLoanDate().isAfter(afterSeed));
            assertTrue(loan.getDueDate().isAfter(loan.getLoanDate()));
            if (loan.isReturned()) {
                assertNotNull(loan.getReturnDate());
                assertFalse(loan.getReturnDate().isBefore(loan.getLoanDate()));
                assertFalse(loan.getReturnDate().isAfter(afterSeed));
            } else {
                assertNull(loan.getReturnDate());
            }
        }
        Reservation reservation = reservations.getFirst();
        assertTrue(bookIds.contains(reservation.getBook().getId()));
        assertTrue(memberIds.contains(reservation.getMember().getId()));
        assertNotNull(reservation.getReservedAt());
        assertFalse(reservation.getReservedAt().isAfter(afterSeed));
        assertFalse(reservation.isFulfilled());
    }

    /**
     * Every open loan consumes one copy of its own book; returned loans consume none.
     *
     * @throws Exception if seeding fails
     */
    @Test
    void seedInventoryCountsOnlyOpenLoans() throws Exception {
        database.seedIfEmpty();
        List<Loan> loans = loanDao.findAll();
        assertEquals(1L, loans.stream().filter(Loan::isReturned).count());
        assertEquals(2L, loans.stream().filter(loan -> !loan.isReturned()).count());

        Map<Integer, Long> openLoansByBook = loans.stream().filter(loan -> !loan.isReturned())
                .collect(Collectors.groupingBy(loan -> loan.getBook().getId(), Collectors.counting()));
        for (Book book : bookDao.findAll()) {
            long openLoans = openLoansByBook.getOrDefault(book.getId(), 0L);
            assertEquals((long) book.getTotalCopies() - openLoans, (long) book.getAvailableCopies(),
                    "Incorrect inventory for book " + book.getId());
            assertTrue(book.getAvailableCopies() >= 0);
            if (openLoans > 0) {
                assertTrue(book.getAvailableCopies() < book.getTotalCopies());
            }
        }
    }

    /**
     * Repeating the seed preserves all identifiers and every stored field, including inventory,
     * foreign keys, nullable return dates, fees, and reservation fulfillment state.
     *
     * @throws Exception if either seed call fails
     */
    @Test
    void repeatedSeedPreservesAllRecordsAndState() throws Exception {
        database.seedIfEmpty();
        Map<Integer, List<Object>> booksBefore = snapshotBooks();
        Map<Integer, List<Object>> membersBefore = snapshotMembers();
        Map<Integer, List<Object>> loansBefore = snapshotLoans();
        Map<Integer, List<Object>> reservationsBefore = snapshotReservations();
        assertEquals(3, booksBefore.size());
        assertEquals(3, membersBefore.size());
        assertEquals(3, loansBefore.size());
        assertEquals(1, reservationsBefore.size());

        database.seedIfEmpty();

        assertEquals(booksBefore, snapshotBooks(), "Seeding again changed the catalog or inventory");
        assertEquals(membersBefore, snapshotMembers(), "Seeding again changed the members");
        assertEquals(loansBefore, snapshotLoans(), "Seeding again changed the loans");
        assertEquals(reservationsBefore, snapshotReservations(), "Seeding again changed the reservations");
    }

    /**
     * Copies all book values into a map keyed by persisted identifier, independent of row order.
     *
     * @return book identifiers mapped to catalog and inventory values
     */
    private Map<Integer, List<Object>> snapshotBooks() {
        return bookDao.findAll().stream().collect(Collectors.toMap(Book::getId,
                book -> List.of(book.getTitle(), book.getAuthor(), book.getIsbn(),
                        book.getTotalCopies(), book.getAvailableCopies())));
    }

    /**
     * Copies all member values without retaining mutable model objects.
     *
     * @return member identifiers mapped to name and email
     */
    private Map<Integer, List<Object>> snapshotMembers() {
        return memberDao.findAll().stream().collect(Collectors.toMap(Member::getId,
                member -> List.of(member.getName(), member.getEmail())));
    }

    /**
     * Copies all loan values; the lists deliberately allow null return dates on open loans.
     *
     * @return loan identifiers mapped to relationships, dates, return state, and fee
     */
    private Map<Integer, List<Object>> snapshotLoans() {
        return loanDao.findAll().stream().collect(Collectors.toMap(Loan::getId,
                loan -> Arrays.asList(loan.getMember().getId(), loan.getBook().getId(), loan.getLoanDate(),
                        loan.getDueDate(), loan.getReturnDate(), loan.isReturned(), loan.getOverdueFee())));
    }

    /**
     * Copies all reservation values without relying on the order returned by the DAO.
     *
     * @return reservation identifiers mapped to relationships, date, and fulfillment state
     */
    private Map<Integer, List<Object>> snapshotReservations() {
        return reservationDao.findAll().stream().collect(Collectors.toMap(Reservation::getId,
                reservation -> List.of(reservation.getMember().getId(), reservation.getBook().getId(),
                        reservation.getReservedAt(), reservation.isFulfilled())));
    }
}
