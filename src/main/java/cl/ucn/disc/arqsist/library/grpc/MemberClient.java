/*
 * Copyright (c) 2026. Arquitectura de Sistemas, DISC, UCN, Antofagasta.
 */
package cl.ucn.disc.arqsist.library.grpc;
import cl.ucn.disc.arqsist.library.model.Member;
import cl.ucn.disc.arqsist.library.service.NotFoundException;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import java.util.List;
import java.util.concurrent.TimeUnit;
/**
 * The gRPC client of the Member Service.
 *
 * The client owns one ManagedChannel and one blocking stub. Every method
 * converts the domain model to a protobuf request and the reply back.
 */
public final class MemberClient implements AutoCloseable {
    /**
     * The deadline of each call, in seconds.
     */
    private static final long DEADLINE_SECONDS = 5;
    /**
     * The channel to the server.
     */
    private final ManagedChannel channel;
    /**
     * The blocking stub. Each call blocks the caller until the reply arrives.
     */
    private final MemberServiceGrpc.MemberServiceBlockingStub stub;
    /**
     * The Constructor.
     *
     * @param host the server host
     * @param port the server port
     */
    public MemberClient(String host, int port) {
        // usePlaintext disables TLS: this lab server has no certificate.
        this(ManagedChannelBuilder.forAddress(host, port).usePlaintext().build());
    }
    /**
     * The Constructor for an existing channel.
     *
     * @param channel the channel to the server
     */
    public MemberClient(ManagedChannel channel) {
        // One channel serves many calls. The stub reuses it.
        this.channel = channel;
        this.stub = MemberServiceGrpc.newBlockingStub(channel);
    }
    /**
     * Register a new member.
     *
     * @param member the member to register
     * @return the registered member
     */
    public Member register(Member member) {
        try {
            // The deadline frees the caller when the server does not answer.
            MemberMessage reply = stub.withDeadlineAfter(DEADLINE_SECONDS, TimeUnit.SECONDS)
                    .registerMember(RegisterMemberRequest.newBuilder()
                            .setName(member.getName())
                            .setEmail(member.getEmail())
                            .build());
            return toMember(reply);
        } catch (StatusRuntimeException e) {
            throw toException(e);
        }
    }
    /**
     * @return a list of all members.
     */
    public List<Member> findAll() {
        try {
            // The request has no fields: use the shared default instance.
            ListMembersReply reply = stub.withDeadlineAfter(DEADLINE_SECONDS, TimeUnit.SECONDS)
                    .listMembers(ListMembersRequest.getDefaultInstance());
            return reply.getMembersList().stream()
                    .map(MemberClient::toMember)
                    .toList();
        } catch (StatusRuntimeException e) {
            throw toException(e);
        }
    }
    /**
     * Find a member by ID.
     *
     * @param id the member ID
     * @return the member
     * @throws NotFoundException if the member does not exist
     */
    public Member findById(int id) {
        try {
            MemberMessage reply = stub.withDeadlineAfter(DEADLINE_SECONDS, TimeUnit.SECONDS)
                    .getMember(GetMemberRequest.newBuilder().setId(id).build());
            return toMember(reply);
        } catch (StatusRuntimeException e) {
            throw toException(e);
        }
    }
    /**
     * Search for members by a literal fragment of their name.
     *
     * @param query fragment to match
     * @return matching members ordered by increasing ID
     * @throws IllegalArgumentException if the query is blank
     */
    public List<Member> search(String query) {
        try {
            ListMembersReply reply = stub.withDeadlineAfter(DEADLINE_SECONDS, TimeUnit.SECONDS)
                    .searchMembers(SearchMembersRequest.newBuilder().setQuery(query).build());
            return reply.getMembersList().stream().map(MemberClient::toMember).toList();
        } catch (StatusRuntimeException e) {
            throw toException(e);
        }
    }
    /**
     * Close the channel. Wait for the running calls to end.
     */
    @Override
    public void close() {
        // shutdown refuses new calls and lets the running calls end.
        channel.shutdown();
        try {
            if (!channel.awaitTermination(DEADLINE_SECONDS, TimeUnit.SECONDS)) {
                // The calls did not end in time: cancel them.
                channel.shutdownNow();
            }
        } catch (InterruptedException e) {
            channel.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    /**
     * Map a gRPC status error to a domain error.
     *
     * @param e the status error from the stub
     * @return the domain error, or the status error when the code has no domain error
     */
    private static RuntimeException toException(StatusRuntimeException e) {
        // The server reports a domain error as a status code. This mirrors toError of the adapter.
        String description = e.getStatus().getDescription();
        return switch (e.getStatus().getCode()) {
            case NOT_FOUND -> new NotFoundException(description);
            case INVALID_ARGUMENT -> new IllegalArgumentException(description);
            case FAILED_PRECONDITION -> new IllegalStateException(description);
            // UNAVAILABLE, DEADLINE_EXCEEDED, and the others travel up unchanged.
            default -> e;
        };
    }
    /**
     * Convert a protobuf message to a domain member.
     *
     * @param message the protobuf message
     * @return the domain member
     */
    private static Member toMember(MemberMessage message) {
        // The domain entity has a constructor with the data and a setter for the id.
        Member member = new Member(message.getName(), message.getEmail());
        // Restore the identifier that the database generated.
        member.setId(message.getId());
        return member;
    }
}
