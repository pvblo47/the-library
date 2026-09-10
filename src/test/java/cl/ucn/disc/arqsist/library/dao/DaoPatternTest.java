package cl.ucn.disc.arqsist.library.dao;

import cl.ucn.disc.arqsist.library.db.Database;
import cl.ucn.disc.arqsist.library.model.Book;
import cl.ucn.disc.arqsist.library.model.Loan;
import cl.ucn.disc.arqsist.library.model.Member;
import cl.ucn.disc.arqsist.library.model.Reservation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for option 4: generic CrudDao<T> + per-entity interfaces + ORMLite adapters.
 */
class DaoPatternTest {

    private Database database;
    private BookDao bookDao;
    private MemberDao memberDao;
    private LoanDao loanDao;
    private ReservationDao reservationDao;

    @BeforeEach
    void setUp() throws Exception {
        database = new Database("jdbc:sqlite::memory:");
        bookDao = new OrmLiteBookDao(database.connectionSource());
        memberDao = new OrmLiteMemberDao(database.connectionSource());
        loanDao = new OrmLiteLoanDao(database.connectionSource());
        reservationDao = new OrmLiteReservationDao(database.connectionSource());
    }

    @AfterEach
    void tearDown() throws Exception {
        database.connectionSource().close();
    }

    @Test
    void bookDaoSupportsCompleteCrud() {
        Book book = new Book("Clean Architecture", "Robert C. Martin", "9780134494166", 2);

        bookDao.create(book);
        assertTrue(book.getId() > 0);
        assertEquals(1, bookDao.findAll().size());
        assertEquals("Clean Architecture", bookDao.findById(book.getId()).getTitle());

        book.setTitle("Clean Architecture - Updated");
        bookDao.update(book);
        assertEquals("Clean Architecture - Updated", bookDao.findById(book.getId()).getTitle());

        bookDao.delete(book);
        assertNull(bookDao.findById(book.getId()));
    }

    @Test
    void memberDaoSupportsCompleteCrud() {
        Member member = new Member("Ada Lovelace", "ada@example.com");

        memberDao.create(member);
        assertTrue(member.getId() > 0);
        assertEquals(1, memberDao.findAll().size());
        assertEquals("Ada Lovelace", memberDao.findById(member.getId()).getName());

        member.setName("Augusta Ada Lovelace");
        memberDao.update(member);
        assertEquals("Augusta Ada Lovelace", memberDao.findById(member.getId()).getName());

        memberDao.delete(member);
        assertNull(memberDao.findById(member.getId()));
    }

    @Test
    void loanDaoSupportsCompleteCrud() {
        Book book = persistedBook();
        Member member = persistedMember();
        String today = LocalDate.now().toString();
        String dueDate = LocalDate.now().plusDays(21).toString();
        Loan loan = new Loan(member, book, today, dueDate);

        loanDao.create(loan);
        assertTrue(loan.getId() > 0);
        assertEquals(1, loanDao.findAll().size());
        assertFalse(loanDao.findById(loan.getId()).isReturned());

        loan.setReturned(true);
        loanDao.update(loan);
        assertTrue(loanDao.findById(loan.getId()).isReturned());

        loanDao.delete(loan);
        assertNull(loanDao.findById(loan.getId()));
    }

    @Test
    void reservationDaoSupportsCompleteCrud() {
        Book book = persistedBook();
        Member member = persistedMember();
        Reservation reservation = new Reservation(member, book, LocalDate.now().toString());

        reservationDao.create(reservation);
        assertTrue(reservation.getId() > 0);
        assertEquals(1, reservationDao.findAll().size());
        assertFalse(reservationDao.findById(reservation.getId()).isFulfilled());

        reservation.setFulfilled(true);
        reservationDao.update(reservation);
        assertTrue(reservationDao.findById(reservation.getId()).isFulfilled());

        reservationDao.delete(reservation);
        assertNull(reservationDao.findById(reservation.getId()));
    }

    @Test
    void ormLiteExceptionsAreHiddenFromDaoClients() {
        Book invalidBook = new Book(null, "Author", "invalid-isbn", 1);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> bookDao.create(invalidBook));
        assertInstanceOf(java.sql.SQLException.class, exception.getCause());
    }

    private Book persistedBook() {
        Book book = new Book("Design Patterns", "Gamma et al.", "9780201633610", 1);
        bookDao.create(book);
        return book;
    }

    private Member persistedMember() {
        Member member = new Member("Grace Hopper", "grace@example.com");
        memberDao.create(member);
        return member;
    }
}
