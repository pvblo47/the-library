/*
 * Copyright (c) 2026. Arquitectura de Sistemas, DISC, UCN, Antofagasta.
 */
package cl.ucn.disc.arqsist.library.service;

import cl.ucn.disc.arqsist.library.dao.BookDao;
import cl.ucn.disc.arqsist.library.dao.LoanDao;
import cl.ucn.disc.arqsist.library.dao.MemberDao;
import cl.ucn.disc.arqsist.library.dao.ReservationDao;
import cl.ucn.disc.arqsist.library.model.Book;
import cl.ucn.disc.arqsist.library.model.Loan;
import cl.ucn.disc.arqsist.library.model.Member;
import cl.ucn.disc.arqsist.library.model.Reservation;

import java.time.LocalDate;
import java.util.List;

/** Creates reservations and turns them into loans. */
public final class ReservationService {

    /** DAO used to read and persist reservations. */
    private final ReservationDao reservationDao;

    /** DAO used to look up books for new reservations. */
    private final BookDao bookDao;

    /** DAO used to look up members for new reservations. */
    private final MemberDao memberDao;

    /** DAO used to persist loans created when a reservation is fulfilled. */
    private final LoanDao loanDao;

    /**
     * Creates a reservation service.
     *
     * @param reservationDao DAO used to read and persist reservations
     * @param bookDao        DAO used to look up books
     * @param memberDao      DAO used to look up members
     * @param loanDao        DAO used to persist loans
     */
    public ReservationService(ReservationDao reservationDao, BookDao bookDao, MemberDao memberDao, LoanDao loanDao) {
        this.reservationDao = reservationDao;
        this.bookDao = bookDao;
        this.memberDao = memberDao;
        this.loanDao = loanDao;
    }

    /**
     * Reserves a book for a member, dated today.
     *
     * @param bookId   identifier of the book to reserve
     * @param memberId identifier of the member who reserves the book
     * @return the persisted reservation
     * @throws RuntimeException if a persistence operation fails
     */
    public Reservation reserve(int bookId, int memberId) {
        Book book = bookDao.findById(bookId);
        Member member = memberDao.findById(memberId);
        Reservation reservation = new Reservation(member, book, LocalDate.now());
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
     * Fulfills a reservation by creating a loan for it. The due date comes from
     * {@link LoanPolicy#dueDate(LocalDate)}.
     *
     * @param reservationId identifier of the reservation to fulfill
     * @return the loan that was created
     * @throws IllegalStateException if the reservation does not exist or is already fulfilled
     * @throws RuntimeException      if a persistence operation fails
     */
    public Loan fulfill(int reservationId) {
        Reservation reservation = reservationDao.findById(reservationId);
        if (reservation == null || reservation.isFulfilled()) {
            throw new IllegalStateException("Reservation not available");
        }

        reservation.setFulfilled(true);
        reservationDao.update(reservation);

        LocalDate today = LocalDate.now();
        LocalDate dueDate = LoanPolicy.dueDate(today);
        Loan loan = new Loan(reservation.getMember(), reservation.getBook(), today, dueDate);
        loanDao.create(loan);
        return loan;
    }
}
