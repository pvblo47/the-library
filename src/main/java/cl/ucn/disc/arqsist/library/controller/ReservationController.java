/*
 * Copyright (c) 2026. Arquitectura de Sistemas, DISC, UCN, Antofagasta.
 */
package cl.ucn.disc.arqsist.library.controller;

import cl.ucn.disc.arqsist.library.service.ReservationService;
import io.javalin.config.JavalinConfig;

/** Registers reservation routes that delegate validation and fulfillment to the service. */
public final class ReservationController {

    /** Service responsible for reservations and transactional fulfillment. */
    private final ReservationService service;

    /**
     * Creates the reservation route adapter.
     *
     * @param service reservation service used by the routes
     */
    public ReservationController(ReservationService service) {
        this.service = service;
    }

    /**
     * Registers reservation creation, listing, and fulfillment routes. Service and parsing
     * exceptions propagate to the server's configured exception handlers.
     *
     * @param config server configuration that receives the routes
     */
    public void register(JavalinConfig config) {
        config.routes.post("/reservations", ctx -> {
            int memberId = Integer.parseInt(ctx.queryParam("memberId"));
            int bookId = Integer.parseInt(ctx.queryParam("bookId"));
            ctx.json(service.reserve(bookId, memberId));
        });
        config.routes.get("/reservations", ctx -> ctx.json(service.findAll()));
        config.routes.post("/reservations/{id}/fulfill", ctx -> ctx.json(service.fulfill(Integer.parseInt(ctx.pathParam("id")))));
    }
}
