/*
 * Copyright (c) 2026. Arquitectura de Sistemas, DISC, UCN, Antofagasta.
 */
package cl.ucn.disc.arqsist.library.controller;

import cl.ucn.disc.arqsist.library.model.Member;
import cl.ucn.disc.arqsist.library.service.MemberService;
import io.javalin.config.JavalinConfig;

/** Registers member listing and registration routes backed by {@link MemberService}. */
public final class MemberController {

    /** Service used to register and list members. */
    private final MemberService service;

    /**
     * Creates the member route adapter.
     *
     * @param service member service used by the routes
     */
    public MemberController(MemberService service) {
        this.service = service;
    }

    /**
     * Registers the existing member listing and registration routes.
     *
     * @param config server configuration that receives the routes
     */
    public void register(JavalinConfig config) {
        config.routes.get("/members", ctx -> ctx.json(service.findAll()));
        config.routes.post("/members", ctx -> ctx.json(service.register(ctx.bodyAsClass(Member.class))));
    }
}
