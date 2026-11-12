/*
 * Copyright (c) 2026. Arquitectura de Sistemas, DISC, UCN, Antofagasta.
 */
package cl.ucn.disc.arqsist.library.grpc;
import cl.ucn.disc.arqsist.library.model.Member;
import cl.ucn.disc.arqsist.library.service.MemberService;
import cl.ucn.disc.arqsist.library.service.NotFoundException;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import java.util.List;
/**
 * The gRPC adapter for the Member Service.
 *
 * The class extends the base class that protoc generates from member.proto.
 * Each unary RPC of the proto file maps to one method below. The method
 * receives the request and a StreamObserver for the reply.
 */
public final class MemberGrpcService extends MemberServiceGrpc.MemberServiceImplBase {
    /**
     * The address of the gRPC server. App and MemberClientApp use it.
     */
    public static final String HOST = "127.0.0.1";
    public static final int PORT = 50051;
    /**
     * The Member Service.
     */
    private final MemberService memberService;
    /**
     * The Constructor.
     *
     * @param memberService the Member Service
     */
    public MemberGrpcService(MemberService memberService) {
        this.memberService = memberService;
    }
    /**
     * Register a new member.
     *
     * @param request  the registration data
     * @param observer the reply observer
     */
    @Override
    public void registerMember(RegisterMemberRequest request, StreamObserver<MemberMessage> observer) {
        try {
            // The protobuf request and the domain entity are different types.
            Member member = memberService.register(new Member(request.getName(), request.getEmail()));
            // A unary RPC sends exactly one reply and then completes the call.
            observer.onNext(toMessage(member));
            observer.onCompleted();
        } catch (RuntimeException e) {
            observer.onError(toError(e));
        }
    }
    /**
     * List all members.
     *
     * @param request  the empty request
     * @param observer the reply observer
     */
    @Override
    public void listMembers(ListMembersRequest request, StreamObserver<ListMembersReply> observer) {
        try {
            // Convert every domain entity to a protobuf message.
            List<MemberMessage> members = memberService.findAll().stream()
                    .map(MemberGrpcService::toMessage)
                    .toList();
            // addAllMembers copies the list into the repeated field of the reply.
            observer.onNext(ListMembersReply.newBuilder().addAllMembers(members).build());
            observer.onCompleted();
        } catch (RuntimeException e) {
            observer.onError(toError(e));
        }
    }
    /**
     * Get one member by ID.
     *
     * @param request  the request with the member ID
     * @param observer the reply observer
     */
    @Override
    public void getMember(GetMemberRequest request, StreamObserver<MemberMessage> observer) {
        try {
            observer.onNext(toMessage(memberService.findById(request.getId())));
            observer.onCompleted();
        } catch (RuntimeException e) {
            observer.onError(toError(e));
        }
    }
    /**
     * Map a domain error to a gRPC status error.
     *
     * @param e the domain error
     * @return the status error for the client
     */
    private static StatusRuntimeException toError(RuntimeException e) {
        // The client reads the status code, not the Java exception type.
        // Put the specific types first: the switch takes the first match.
        Status status = switch (e) {
            case NotFoundException x -> Status.NOT_FOUND;
            case IllegalArgumentException x -> Status.INVALID_ARGUMENT;
            case IllegalStateException x -> Status.FAILED_PRECONDITION;
            // An unmapped exception is a server bug.
            default -> Status.INTERNAL;
        };
        return status.withDescription(e.getMessage()).withCause(e).asRuntimeException();
    }
    /**
     * Convert a domain member to a protobuf message.
     *
     * @param member the domain member
     * @return the protobuf message
     */
    private static MemberMessage toMessage(Member member) {
        // Copy each field: the domain model is not the wire format.
        return MemberMessage.newBuilder()
                .setId(member.getId())
                .setName(member.getName())
                .setEmail(member.getEmail())
                .build();
    }
}
