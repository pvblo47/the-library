/*
 * Copyright (c) 2026. Arquitectura de Sistemas, DISC, UCN, Antofagasta.
 */
package cl.ucn.disc.arqsist.library.controller;

import cl.ucn.disc.arqsist.library.model.Book;
import cl.ucn.disc.arqsist.library.service.BookService;
import io.javalin.config.JavalinConfig;

/** Registers catalog routes that delegate all persistence operations to {@link BookService}. */
public final class BookController {

    /** Service used to list, find, and create books. */
    private final BookService service;

    /**
     * Creates the catalog route adapter.
     *
     * @param service catalog service used by every book route
     */
    public BookController(BookService service) {
        this.service = service;
    }

    /**
     * Registers book listing, lookup, and creation routes. Service and parsing exceptions
     * propagate to the server's configured exception handlers.
     *
     * @param config server configuration that receives the routes
     */
    public void register(JavalinConfig config) {
        config.routes.get("/books", ctx -> ctx.json(service.listAll()));
        config.routes.get("/books/{id}", ctx -> ctx.json(service.findById(Integer.parseInt(ctx.pathParam("id")))));
        config.routes.post("/books", ctx -> ctx.json(service.create(ctx.bodyAsClass(Book.class))));
    }
}
