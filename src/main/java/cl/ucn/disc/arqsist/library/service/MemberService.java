/*
 * Copyright (c) 2026. Arquitectura de Sistemas, DISC, UCN, Antofagasta.
 */
package cl.ucn.disc.arqsist.library.service;

import cl.ucn.disc.arqsist.library.dao.BookDao;
import cl.ucn.disc.arqsist.library.dao.LoanDao;
import cl.ucn.disc.arqsist.library.dao.MemberDao;
import cl.ucn.disc.arqsist.library.model.Book;
import cl.ucn.disc.arqsist.library.model.Loan;
import cl.ucn.disc.arqsist.library.model.Member;

import java.time.LocalDate;
import java.util.List;

/** Registers members and, until Stage 2 moves it, performs book checkouts. */
public final class MemberService {

    /** DAO used to read and persist members. */
    private final MemberDao memberDao;

    /** DAO used to read and update the copy count of books. */
    private final BookDao bookDao;

    /** DAO used to persist loans created by a checkout. */
    private final LoanDao loanDao;

    /**
     * Creates a member service.
     *
     * @param memberDao DAO used to read and persist members
     * @param bookDao   DAO used to read and update books
     * @param loanDao   DAO used to persist loans
     */
    public MemberService(MemberDao memberDao, BookDao bookDao, LoanDao loanDao) {
        this.memberDao = memberDao;
        this.bookDao = bookDao;
        this.loanDao = loanDao;
    }

    /**
     * Registers a new member.
     *
     * @param member member to persist
     * @return the persisted member, with its generated identifier
     * @throws RuntimeException if the member cannot be persisted
     */
    public Member register(Member member) {
        memberDao.create(member);
        return member;
    }

    /**
     * Lists all registered members.
     *
     * @return all members
     * @throws RuntimeException if the members cannot be read
     */
    public List<Member> findAll() {
        return memberDao.findAll();
    }

    /**
     * Checks out a book to a member. The due date comes from {@link LoanPolicy#dueDate(LocalDate)}.
     *
     * @param memberId identifier of the member who borrows the book
     * @param bookId   identifier of the book to borrow
     * @return the loan that was created
     * @throws IllegalArgumentException if the member or the book does not exist
     * @throws RuntimeException         if a persistence operation fails
     */
    public Loan checkout(int memberId, int bookId) {
        Member member = memberDao.findById(memberId);
        if (member == null) {
            throw new IllegalArgumentException("Member not found: " + memberId);
        }

        Book book = bookDao.findById(bookId);
        if (book == null) {
            throw new IllegalArgumentException("Book not found: " + bookId);
        }

        book.setAvailableCopies(book.getAvailableCopies() - 1);
        bookDao.update(book);

        LocalDate today = LocalDate.now();
        String dueDate = LoanPolicy.dueDate(today).toString();
        Loan loan = new Loan(member, book, today.toString(), dueDate);
        loanDao.create(loan);
        return loan;
    }
}
