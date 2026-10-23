/*
 * Copyright (c) 2026. Arquitectura de Sistemas, DISC, UCN, Antofagasta.
 */
package cl.ucn.disc.arqsist.library.service;

import cl.ucn.disc.arqsist.library.dao.MemberDao;
import cl.ucn.disc.arqsist.library.model.Member;

import java.util.List;

/** Registers members and lists the membership catalog. */
public final class MemberService {

    /** DAO used to read and persist members. */
    private final MemberDao memberDao;

    /**
     * Creates a member service.
     *
     * @param memberDao DAO used to read and persist members
     */
    public MemberService(MemberDao memberDao) {
        this.memberDao = memberDao;
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

}
