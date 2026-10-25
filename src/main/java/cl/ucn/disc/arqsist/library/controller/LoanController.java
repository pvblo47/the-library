/*
 * Copyright (c) 2026. Arquitectura de Sistemas, DISC, UCN, Antofagasta.
 */
package cl.ucn.disc.arqsist.library.controller;

import cl.ucn.disc.arqsist.library.service.LoanService;
import io.javalin.config.JavalinConfig;

/** Registers HTTP routes that delegate lending operations to {@link LoanService}. */
public final class LoanController {

    /** Service responsible for loans and their inventory transactions. */
    private final LoanService loanService;

    /**
     * Creates the loan route adapter.
     *
     * @param loanService service that handles lending operations
     */
    public LoanController(LoanService loanService) {
        this.loanService = loanService;
    }

    /**
     * Registers loan creation, listing, return, and overdue routes.
     * Service exceptions propagate to Javalin's configured exception handlers.
     *
     * @param config server configuration that receives the routes
     */
    public void register(JavalinConfig config) {
        config.routes.post("/loans", ctx -> {
            int memberId = Integer.parseInt(ctx.queryParam("memberId"));
            int bookId = Integer.parseInt(ctx.queryParam("bookId"));
            ctx.json(loanService.checkout(memberId, bookId));
        });
        config.routes.get("/loans", ctx -> ctx.json(loanService.findAll()));
        config.routes.post("/loans/{id}/return", ctx -> ctx.json(loanService.returnLoan(Integer.parseInt(ctx.pathParam("id")))));
        config.routes.get("/loans/overdue", ctx -> ctx.json(loanService.overdueLoans()));
    }
}
