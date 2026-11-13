/*
 * Copyright (c) 2026. Arquitectura de Sistemas, DISC, UCN, Antofagasta.
 */
package cl.ucn.disc.arqsist.library.grpc;

import cl.ucn.disc.arqsist.library.dao.BaseDao;
import cl.ucn.disc.arqsist.library.dao.MemberDao;
import cl.ucn.disc.arqsist.library.dao.OrmLiteMemberDao;
import cl.ucn.disc.arqsist.library.db.Database;
import cl.ucn.disc.arqsist.library.model.Member;
import cl.ucn.disc.arqsist.library.service.MemberService;
import cl.ucn.disc.arqsist.library.service.NotFoundException;
import com.j256.ormlite.support.ConnectionSource;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests the adapter's wire messages and status mappings without the later MemberClient. */
class MemberGrpcServiceTest {

    /** Isolated database owned by each test. */
    private Database database;

    /** Real ORMLite-backed DAO with an optional test-only failure switch. */
    private ControllableMemberDao dao;

    /** In-process gRPC server, never bound to an application port. */
    private Server server;

    /** In-process channel used by the generated stub. */
    private ManagedChannel channel;

    /** Generated stub used directly, without introducing MemberClient. */
    private MemberServiceGrpc.MemberServiceBlockingStub stub;

    /**
     * Creates the database, domain service, adapter, and uniquely named in-process transport.
     *
     * @throws Exception if database or server initialization fails
     */
    @BeforeEach
    void setUp() throws Exception {
        database = new Database("jdbc:sqlite::memory:");
        dao = new ControllableMemberDao(database.connectionSource());
        MemberService service = new MemberService(dao);
        String serverName = InProcessServerBuilder.generateName();
        server = InProcessServerBuilder.forName(serverName).directExecutor()
                .addService(new MemberGrpcService(service)).build().start();
        channel = InProcessChannelBuilder.forName(serverName).directExecutor().build();
        stub = MemberServiceGrpc.newBlockingStub(channel);
    }

    /**
     * Closes the channel, server, and database even if another cleanup step fails.
     *
     * @throws Exception if transport termination or database cleanup fails
     */
    @AfterEach
    void tearDown() throws Exception {
        try {
            if (channel != null) {
                channel.shutdownNow();
                assertTrue(channel.awaitTermination(5, TimeUnit.SECONDS));
            }
        } finally {
            try {
                if (server != null) {
                    server.shutdownNow();
                    assertTrue(server.awaitTermination(5, TimeUnit.SECONDS));
                }
            } finally {
                if (database != null) {
                    database.connectionSource().close();
                }
            }
        }
    }

    /** Registration returns one complete message containing the identifier and stored fields. */
    @Test
    void registerMemberReturnsThePersistedMember() {
        MemberMessage reply = rpc().registerMember(RegisterMemberRequest.newBuilder()
                .setName("Ada Lovelace").setEmail("ada@example.com").build());

        assertTrue(reply.getId() > 0);
        assertEquals("Ada Lovelace", reply.getName());
        assertEquals("ada@example.com", reply.getEmail());
        Member stored = dao.findById(reply.getId());
        assertEquals(reply.getName(), stored.getName());
        assertEquals(reply.getEmail(), stored.getEmail());
        assertEquals(1, dao.findAll().size());
    }

    /** Listing supports an empty database and copies every field without assuming row order. */
    @Test
    void listMembersReturnsEmptyAndPopulatedReplies() {
        assertEquals(0, rpc().listMembers(ListMembersRequest.getDefaultInstance()).getMembersCount());
        Member ada = new Member("Ada Lovelace", "ada@example.com");
        Member grace = new Member("Grace Hopper", "grace@example.com");
        dao.create(ada);
        dao.create(grace);

        ListMembersReply reply = rpc().listMembers(ListMembersRequest.getDefaultInstance());
        assertEquals(2, reply.getMembersCount());
        Map<Integer, MemberMessage> members = reply.getMembersList().stream()
                .collect(Collectors.toMap(MemberMessage::getId, member -> member));
        assertEquals(Set.of(ada.getId(), grace.getId()), members.keySet());
        assertEquals("Ada Lovelace", members.get(ada.getId()).getName());
        assertEquals("ada@example.com", members.get(ada.getId()).getEmail());
        assertEquals("Grace Hopper", members.get(grace.getId()).getName());
        assertEquals("grace@example.com", members.get(grace.getId()).getEmail());
    }

    /** Lookup copies the stored identifier, name, and email into the reply message. */
    @Test
    void getMemberReturnsTheRequestedMember() {
        Member member = new Member("Alan Turing", "alan@example.com");
        dao.create(member);

        MemberMessage reply = rpc().getMember(GetMemberRequest.newBuilder().setId(member.getId()).build());

        assertEquals(member.getId(), reply.getId());
        assertEquals("Alan Turing", reply.getName());
        assertEquals("alan@example.com", reply.getEmail());
    }

    /** A missing database row crosses the wire as NOT_FOUND with the original message. */
    @Test
    void getMissingMemberReturnsNotFound() {
        assertStatus(() -> rpc().getMember(GetMemberRequest.newBuilder().setId(9999).build()),
                Status.Code.NOT_FOUND, "Member not found: 9999");
        assertTrue(dao.findAll().isEmpty());
    }

    /**
     * Every RPC reports the mapped wire code and description instead of an uncaught UNKNOWN.
     *
     * @param failure domain or infrastructure error produced by the DAO test double
     * @param expectedCode wire status required by the guided mapping
     */
    @ParameterizedTest
    @MethodSource("domainErrors")
    void mapsErrorsFromEveryRpc(RuntimeException failure, Status.Code expectedCode) {
        dao.failure = failure;

        assertStatus(() -> rpc().registerMember(RegisterMemberRequest.newBuilder()
                        .setName("Ada Lovelace").setEmail("ada@example.com").build()),
                expectedCode, failure.getMessage());
        assertStatus(() -> rpc().listMembers(ListMembersRequest.getDefaultInstance()),
                expectedCode, failure.getMessage());
        assertStatus(() -> rpc().getMember(GetMemberRequest.newBuilder().setId(9999).build()),
                expectedCode, failure.getMessage());
        assertStatus(() -> rpc().searchMembers(SearchMembersRequest.newBuilder().setQuery("Ada").build()),
                expectedCode, failure.getMessage());

        dao.failure = null;
        assertEquals(0, rpc().listMembers(ListMembersRequest.getDefaultInstance()).getMembersCount());
    }

    /**
     * Supplies the four error categories without adding production validation or dependencies.
     *
     * @return exceptions paired with their required gRPC codes
     */
    static Stream<Arguments> domainErrors() {
        return Stream.of(
                Arguments.of(new NotFoundException("missing member"), Status.Code.NOT_FOUND),
                Arguments.of(new IllegalArgumentException("invalid member"), Status.Code.INVALID_ARGUMENT),
                Arguments.of(new IllegalStateException("member operation unavailable"), Status.Code.FAILED_PRECONDITION),
                Arguments.of(new RuntimeException("database failure"), Status.Code.INTERNAL));
    }

    /**
     * Creates a bounded view of the stub so a broken unary completion cannot hang the suite.
     *
     * @return stub with a fresh five-second deadline for this test call
     */
    private MemberServiceGrpc.MemberServiceBlockingStub rpc() {
        return stub.withDeadlineAfter(5, TimeUnit.SECONDS);
    }

    /**
     * Verifies the client receives a wire status and its description.
     *
     * @param invocation RPC call expected to fail
     * @param expectedCode required wire code
     * @param expectedDescription required original exception message
     */
    private void assertStatus(Runnable invocation, Status.Code expectedCode, String expectedDescription) {
        StatusRuntimeException error = assertThrows(StatusRuntimeException.class, invocation::run);
        assertEquals(expectedCode, error.getStatus().getCode());
        assertEquals(expectedDescription, error.getStatus().getDescription());
    }

    /** Real shared DAO implementation whose adapter-facing operations can fail on demand. */
    private static final class ControllableMemberDao extends BaseDao<Member> implements MemberDao {

        /** Error thrown before a read or write, or null to use actual persistence. */
        private RuntimeException failure;

        /** Production adapter for member-specific queries. */
        private final OrmLiteMemberDao memberQueries;

        /**
         * Creates a test adapter against the same connection source as the domain service.
         *
         * @param connectionSource in-memory database connection source
         */
        private ControllableMemberDao(ConnectionSource connectionSource) {
            super(connectionSource, Member.class);
            memberQueries = new OrmLiteMemberDao(connectionSource);
        }

        /** {@inheritDoc} */
        @Override
        public List<Member> search(String query) {
            failIfConfigured();
            return memberQueries.search(query);
        }

        /**
         * Stores a member unless the test has configured a failure.
         *
         * @param member member to persist
         * @throws RuntimeException if configured to fail or persistence fails
         */
        @Override
        public void create(Member member) {
            failIfConfigured();
            super.create(member);
        }

        /**
         * Lists members unless the test has configured a failure.
         *
         * @return members read from the database
         * @throws RuntimeException if configured to fail or persistence fails
         */
        @Override
        public List<Member> findAll() {
            failIfConfigured();
            return super.findAll();
        }

        /**
         * Reads one member unless the test has configured a failure.
         *
         * @param id member identifier
         * @return the persisted member, or null when absent
         * @throws RuntimeException if configured to fail or persistence fails
         */
        @Override
        public Member findById(Integer id) {
            failIfConfigured();
            return super.findById(id);
        }

        /**
         * Raises the configured error without changing any production DAO behavior.
         *
         * @throws RuntimeException the error supplied by the current test
         */
        private void failIfConfigured() {
            if (failure != null) {
                throw failure;
            }
        }
    }
}
