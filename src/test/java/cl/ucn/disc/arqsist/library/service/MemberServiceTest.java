/*
 * Copyright (c) 2026. Arquitectura de Sistemas, DISC, UCN, Antofagasta.
 */
package cl.ucn.disc.arqsist.library.service;

import cl.ucn.disc.arqsist.library.dao.MemberDao;
import cl.ucn.disc.arqsist.library.dao.OrmLiteMemberDao;
import cl.ucn.disc.arqsist.library.db.Database;
import cl.ucn.disc.arqsist.library.model.Member;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Tests member lookup through the existing DAO contract and an isolated database. */
class MemberServiceTest {

    /** In-memory database owned by each test. */
    private Database database;

    /** Member service under test. */
    private MemberService service;

    /**
     * Creates the existing ORMLite adapter and the service against an empty database.
     *
     * @throws Exception if database initialization fails
     */
    @BeforeEach
    void setUp() throws Exception {
        database = new Database("jdbc:sqlite::memory:");
        MemberDao dao = new OrmLiteMemberDao(database.connectionSource());
        service = new MemberService(dao);
    }

    /**
     * Releases the database even when an assertion fails.
     *
     * @throws Exception if the connection cannot be closed
     */
    @AfterEach
    void tearDown() throws Exception {
        if (database != null) {
            database.connectionSource().close();
        }
    }

    /** Lookup returns the saved fields and leaves registration and listing intact. */
    @Test
    void findByIdReturnsTheRegisteredMember() {
        Member registered = service.register(new Member("Ada Lovelace", "ada@example.com"));

        Member found = service.findById(registered.getId());

        assertEquals(registered.getId(), found.getId());
        assertEquals("Ada Lovelace", found.getName());
        assertEquals("ada@example.com", found.getEmail());
        assertEquals(1, service.findAll().size());
        assertEquals(registered.getId(), service.findAll().getFirst().getId());
    }

    /** A missing identifier reports the domain error without changing existing members. */
    @Test
    void findByIdRejectsAMissingMember() {
        Member registered = service.register(new Member("Grace Hopper", "grace@example.com"));

        NotFoundException error = assertThrows(NotFoundException.class, () -> service.findById(9999));

        assertEquals("Member not found: 9999", error.getMessage());
        assertEquals(1, service.findAll().size());
        assertEquals(registered.getId(), service.findAll().getFirst().getId());
    }
}
