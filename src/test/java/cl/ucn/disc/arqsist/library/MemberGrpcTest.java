/*
 * Copyright (c) 2026. Arquitectura de Sistemas, DISC, UCN, Antofagasta.
 */
package cl.ucn.disc.arqsist.library;

import cl.ucn.disc.arqsist.library.dao.OrmLiteMemberDao;
import cl.ucn.disc.arqsist.library.db.Database;
import cl.ucn.disc.arqsist.library.grpc.MemberClient;
import cl.ucn.disc.arqsist.library.grpc.MemberGrpcService;
import cl.ucn.disc.arqsist.library.model.Member;
import cl.ucn.disc.arqsist.library.service.MemberService;
import cl.ucn.disc.arqsist.library.service.NotFoundException;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests the member gRPC round trip.
 */
final class MemberGrpcTest {
    /**
     * Register a member, list the members, and get one member.
     *
     * @throws Exception if the server or database operation fails
     */
    @Test
    void registerListAndGetMember() throws Exception {
        // The in-memory database keeps the test isolated.
        Database db = new Database("jdbc:sqlite::memory:");
        try (var connectionSource = db.connectionSource()) {
            MemberService memberService = new MemberService(new OrmLiteMemberDao(connectionSource));
            // The in-process transport removes the network from the test.
            // directExecutor runs every callback on the test thread.
            String serverName = InProcessServerBuilder.generateName();
            Server server = InProcessServerBuilder.forName(serverName).directExecutor()
                    .addService(new MemberGrpcService(memberService))
                    .build()
                    .start();
            try {
                ManagedChannel channel = InProcessChannelBuilder.forName(serverName).directExecutor().build();
                try (MemberClient client = new MemberClient(channel)) {
                    Member registered = client.register(new Member("Ada Lovelace", "ada@example.com"));
                    assertEquals("Ada Lovelace", registered.getName());

                    List<Member> members = client.findAll();
                    assertEquals(1, members.size());
                    assertEquals(registered.getId(), members.getFirst().getId());

                    Member found = client.findById(registered.getId());
                    assertEquals("ada@example.com", found.getEmail());
                    assertThrows(NotFoundException.class, () -> client.findById(9999));
                }
            } finally {
                // Stop the server: the test owns its lifecycle.
                server.shutdownNow();
            }
        }
    }
}
