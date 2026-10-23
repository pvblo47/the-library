/*
 * Copyright (c) 2026. Arquitectura de Sistemas, DISC, UCN, Antofagasta.
 */
package cl.ucn.disc.arqsist.library.service;

import cl.ucn.disc.arqsist.library.dao.LoanDao;
import cl.ucn.disc.arqsist.library.dao.MemberDao;
import cl.ucn.disc.arqsist.library.dao.ReservationDao;
import cl.ucn.disc.arqsist.library.model.Book;
import cl.ucn.disc.arqsist.library.model.Loan;
import cl.ucn.disc.arqsist.library.model.Member;
import cl.ucn.disc.arqsist.library.model.Reservation;

import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

/** Creates clock-dated reservations and fulfills them with transactional inventory updates. */
public final class ReservationService {

    /** DAO used to read and persist reservations. */
    private final ReservationDao reservationDao;

    /** DAO used to look up members for new reservations. */
    private final MemberDao memberDao;

    /** DAO used to persist loans created when a reservation is fulfilled. */
    private final LoanDao loanDao;

    /** Catalog service used to look up books and borrow copies. */
    private final BookService bookService;

    /** Clock used for reservation and loan dates. */
    private final Clock clock;

    /**
     * Creates a reservation service.
     *
     * @param reservationDao DAO used to read and persist reservations
     * @param memberDao      DAO used to look up members
     * @param loanDao        DAO used to persist loans
     * @param bookService    service used to look up books and borrow copies
     * @param clock          clock that supplies the current date
     */
    public ReservationService(ReservationDao reservationDao, MemberDao memberDao, LoanDao loanDao,
                              BookService bookService, Clock clock) {
        this.reservationDao = reservationDao;
        this.memberDao = memberDao;
        this.loanDao = loanDao;
        this.bookService = bookService;
        this.clock = clock;
    }

    /**
     * Reserves a book for a member, dated today.
     *
     * @param bookId   identifier of the book to reserve
     * @param memberId identifier of the member who reserves the book
     * @return the persisted reservation
     * @throws NotFoundException if the book or member does not exist
     * @throws RuntimeException if a persistence operation fails
     */
    public Reservation reserve(int bookId, int memberId) {
        Book book = bookService.findById(bookId);
        if (book == null) {
            throw new NotFoundException("Book not found: " + bookId);
        }
        Member member = memberDao.findById(memberId);
        if (member == null) {
            throw new NotFoundException("Member not found: " + memberId);
        }
        Reservation reservation = new Reservation(member, book, LocalDate.now(clock));
        reservationDao.create(reservation);
        return reservation;
    }

    /**
     * Lists all reservations.
     *
     * @return all reservations
     * @throws RuntimeException if the reservations cannot be read
     */
    public List<Reservation> findAll() {
        return reservationDao.findAll();
    }

    /**
     * Marks a reservation fulfilled, borrows a copy, and creates a loan in one transaction.
     * The due date comes from {@link LoanPolicy#dueDate(LocalDate)}. All persistence
     * collaborators must share the same database connection source.
     *
     * @param reservationId identifier of the reservation to fulfill
     * @return the loan that was created
     * @throws NotFoundException if the reservation or its book does not exist
     * @throws IllegalStateException if the reservation is fulfilled or no copy is available
     * @throws SQLException if the transaction fails for a database reason
     * @throws RuntimeException      if a persistence operation fails
     */
    public Loan fulfill(int reservationId) throws SQLException {
        return reservationDao.transaction(() -> {
            Reservation reservation = reservationDao.findById(reservationId);
            if (reservation == null) {
                throw new NotFoundException("Reservation not found: " + reservationId);
            }
            if (reservation.isFulfilled()) {
                throw new IllegalStateException("Reservation already fulfilled: " + reservationId);
            }

            LocalDate today = LocalDate.now(clock);
            reservation.setFulfilled(true);
            reservationDao.update(reservation);

            int bookId = reservation.getBook().getId();
            bookService.borrow(bookId);
            Loan loan = new Loan(reservation.getMember(), bookService.findById(bookId),
                    today, LoanPolicy.dueDate(today));
            loanDao.create(loan);
            return loan;
        });
    }
}
