/*
 * Copyright (c) 2026. Arquitectura de Sistemas, DISC, UCN, Antofagasta.
 */
package cl.ucn.disc.arqsist.library.db;

import cl.ucn.disc.arqsist.library.model.Book;
import cl.ucn.disc.arqsist.library.model.Loan;
import cl.ucn.disc.arqsist.library.model.Member;
import cl.ucn.disc.arqsist.library.model.Reservation;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/** Opens the database, creates its tables and seeds it with sample data. */
public final class Database {

    /** Logger that records each step of the seed. */
    private static final Logger log = LoggerFactory.getLogger(Database.class);

    /** Connection source shared by every DAO. */
    private final ConnectionSource connectionSource;

    /**
     * Opens the database and creates the tables that do not exist yet.
     *
     * @param jdbcUrl JDBC URL of the database
     * @throws SQLException if the connection or a table creation fails
     */
    public Database(String jdbcUrl) throws SQLException {
        this.connectionSource = new JdbcConnectionSource(jdbcUrl);
        TableUtils.createTableIfNotExists(connectionSource, Book.class);
        TableUtils.createTableIfNotExists(connectionSource, Member.class);
        TableUtils.createTableIfNotExists(connectionSource, Loan.class);
        TableUtils.createTableIfNotExists(connectionSource, Reservation.class);
    }

    /**
     * Returns the connection source of the database.
     *
     * @return the connection source
     */
    public ConnectionSource connectionSource() {
        return connectionSource;
    }

    /**
     * Fills each empty table with sample data. Tables that already have rows are left unchanged, so
     * calling this method again does not duplicate records. The seed has three members, one
     * reservation and three loans: one returned, one active and one overdue. Only the active and the
     * overdue loan take a copy out of the available stock.
     *
     * @throws SQLException if a read or a write fails
     */
    public void seedIfEmpty() throws SQLException {
        Dao<Book, Integer> bookDao = DaoManager.createDao(connectionSource, Book.class);
        if (bookDao.queryForAll().isEmpty()) {
            log.debug("Seeding books");
            bookDao.create(new Book("Clean Code", "Robert C. Martin", "9780132350884", 3));
            bookDao.create(new Book("The Pragmatic Programmer", "Hunt & Thomas", "9780201616224", 2));
            bookDao.create(new Book("Design Patterns", "Gamma et al.", "9780201633610", 4));
            log.debug("Seeded 3 books");
        } else {
            log.debug("Books already seeded, skipping");
        }

        Dao<Member, Integer> memberDao = DaoManager.createDao(connectionSource, Member.class);
        if (memberDao.queryForAll().isEmpty()) {
            log.debug("Seeding members");
            memberDao.create(new Member("Ada Lovelace", "ada@example.com"));
            memberDao.create(new Member("Grace Hopper", "grace@example.com"));
            memberDao.create(new Member("Alan Turing", "alan@example.com"));
            log.debug("Seeded 3 members");
        } else {
            log.debug("Members already seeded, skipping");
        }

        Dao<Loan, Integer> loanDao = DaoManager.createDao(connectionSource, Loan.class);
        if (loanDao.queryForAll().isEmpty()) {
            log.debug("Seeding loans");
            List<Book> books = bookDao.queryForAll();
            List<Member> members = memberDao.queryForAll();
            LocalDate today = LocalDate.now();

            Loan returned = new Loan(members.getFirst(), books.getFirst(), today.minusDays(30), today.minusDays(9));
            returned.setReturned(true);
            returned.setReturnDate(today.minusDays(10));
            loanDao.create(returned);
            log.debug("Created returned loan {}; the available copies of book {} are unchanged",
                    returned.getId(), returned.getBook().getId());

            Book activeBook = books.get(2);
            Loan active = new Loan(members.get(1), activeBook, today.minusDays(2), today.plusDays(12));
            loanDao.create(active);
            activeBook.setAvailableCopies(activeBook.getAvailableCopies() - 1);
            bookDao.update(activeBook);
            log.debug("Created active loan {}; book {} now has {} available copies",
                    active.getId(), activeBook.getId(), activeBook.getAvailableCopies());

            Book overdueBook = books.get(1);
            Loan overdue = new Loan(members.get(2), overdueBook, today.minusDays(30), today.minusDays(9));
            loanDao.create(overdue);
            overdueBook.setAvailableCopies(overdueBook.getAvailableCopies() - 1);
            bookDao.update(overdueBook);
            log.debug("Created overdue loan {}; book {} now has {} available copies",
                    overdue.getId(), overdueBook.getId(), overdueBook.getAvailableCopies());
        } else {
            log.debug("Loans already seeded, skipping");
        }

        Dao<Reservation, Integer> reservationDao = DaoManager.createDao(connectionSource, Reservation.class);
        if (reservationDao.queryForAll().isEmpty()) {
            log.debug("Seeding reservations");
            List<Book> books = bookDao.queryForAll();
            List<Member> members = memberDao.queryForAll();
            LocalDate today = LocalDate.now();

            reservationDao.create(new Reservation(members.get(0), books.get(1), today.minusDays(1)));
            log.debug("Seeded 1 reservation");
        } else {
            log.debug("Reservations already seeded, skipping");
        }
    }
}
