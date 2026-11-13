/*
 * Copyright (c) 2026. Arquitectura de Sistemas, DISC, UCN, Antofagasta.
 */
package cl.ucn.disc.arqsist.library;

import cl.ucn.disc.arqsist.library.dao.OrmLiteMemberDao;
import cl.ucn.disc.arqsist.library.db.Database;
import cl.ucn.disc.arqsist.library.grpc.MemberClient;
import cl.ucn.disc.arqsist.library.grpc.MemberGrpcService;
import cl.ucn.disc.arqsist.library.grpc.MemberServiceGrpc;
import cl.ucn.disc.arqsist.library.grpc.SearchMembersRequest;
import cl.ucn.disc.arqsist.library.model.Member;
import cl.ucn.disc.arqsist.library.service.MemberService;
import io.grpc.Server;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Exercises name searches through the client, adapter, and SQLite query. */
final class MemberSearchTest {
    private Database database;
    private Server server;
    private MemberClient client;
    private MemberServiceGrpc.MemberServiceBlockingStub stub;
    private List<Member> registered;

    /**
     * Registers the three acceptance-case members in a fresh catalog.
     *
     * @throws Exception if database or server setup fails
     */
    @BeforeEach
    void setUp() throws Exception {
        database = new Database("jdbc:sqlite::memory:");
        MemberService service = new MemberService(new OrmLiteMemberDao(database.connectionSource()));
        String name = InProcessServerBuilder.generateName();
        server = InProcessServerBuilder.forName(name).directExecutor()
                .addService(new MemberGrpcService(service)).build().start();
        var channel = InProcessChannelBuilder.forName(name).directExecutor().build();
        client = new MemberClient(channel);
        stub = MemberServiceGrpc.newBlockingStub(channel);
        registered = List.of(
                client.register(new Member("Ada Lovelace", "ada@example.com")),
                client.register(new Member("Grace Hopper", "grace@example.com")),
                client.register(new Member("Alan Turing", "alan@example.com")));
    }

    /**
     * Releases the channel, server, and connection after each case.
     *
     * @throws Exception if the database connection cannot be closed
     */
    @AfterEach
    void tearDown() throws Exception {
        try {
            if (client != null) {
                client.close();
            }
        } finally {
            try {
                if (server != null) {
                    server.shutdownNow();
                }
            } finally {
                if (database != null) {
                    database.connectionSource().close();
                }
            }
        }
    }

    /**
     * Verifies case folding and surrounding whitespace.
     *
     * @param query spelling of Ada's name fragment
     */
    @ParameterizedTest
    @ValueSource(strings = {"ada", "ADA", " ada "})
    void findsOnlyAda(String query) {
        List<Member> matches = client.search(query);
        assertEquals(List.of("Ada Lovelace"), matches.stream().map(Member::getName).toList());
        assertEquals(registered.getFirst().getId(), matches.getFirst().getId());
    }

    /** Checks all three generated IDs in registration order, which differs from name order. */
    @Test
    void returnsMatchesInIdOrder() {
        assertEquals(registered.stream().map(Member::getId).toList(),
                client.search("a").stream().map(Member::getId).toList());
    }

    /**
     * Confirms that unmatched strings return an empty successful reply.
     *
     * @param query literal fragment absent from the catalog
     */
    @ParameterizedTest
    @ValueSource(strings = {"zzz", "%", "_"})
    void returnsEmptyWhenNoNameMatches(String query) {
        assertEquals(List.of(), client.search(query));
    }

    /** Checks that percent and underscore each match their own literal character. */
    @Test
    void findsNamesContainingLiteralPunctuation() {
        Member percent = client.register(new Member("Sam 10%", "sam@example.com"));
        Member underscore = client.register(new Member("Alex_B", "alex@example.com"));
        assertEquals(List.of(percent.getId()), client.search("%").stream().map(Member::getId).toList());
        assertEquals(List.of(underscore.getId()), client.search("_").stream().map(Member::getId).toList());
    }

    /**
     * Checks the status on the wire and its translation at the client boundary.
     *
     * @param query empty or whitespace-only fragment
     */
    @ParameterizedTest
    @ValueSource(strings = {"", " ", "\t\n"})
    void rejectsBlankQuery(String query) {
        SearchMembersRequest request = SearchMembersRequest.newBuilder().setQuery(query).build();
        StatusRuntimeException wire = assertThrows(StatusRuntimeException.class,
                () -> stub.withDeadlineAfter(5, TimeUnit.SECONDS).searchMembers(request));
        assertEquals(Status.Code.INVALID_ARGUMENT, wire.getStatus().getCode());
        IllegalArgumentException domain = assertThrows(IllegalArgumentException.class, () -> client.search(query));
        assertEquals(wire.getStatus().getDescription(), domain.getMessage());
    }
}
