/*
 * Copyright (c) 2026. Arquitectura de Sistemas, DISC, UCN, Antofagasta.
 */
package cl.ucn.disc.arqsist.library;

import cl.ucn.disc.arqsist.library.controller.BookController;
import cl.ucn.disc.arqsist.library.controller.LoanController;
import cl.ucn.disc.arqsist.library.controller.MemberController;
import cl.ucn.disc.arqsist.library.controller.ReservationController;
import cl.ucn.disc.arqsist.library.dao.BookDao;
import cl.ucn.disc.arqsist.library.dao.LoanDao;
import cl.ucn.disc.arqsist.library.dao.MemberDao;
import cl.ucn.disc.arqsist.library.dao.ReservationDao;
import cl.ucn.disc.arqsist.library.dao.OrmLiteBookDao;
import cl.ucn.disc.arqsist.library.dao.OrmLiteLoanDao;
import cl.ucn.disc.arqsist.library.dao.OrmLiteMemberDao;
import cl.ucn.disc.arqsist.library.dao.OrmLiteReservationDao;
import cl.ucn.disc.arqsist.library.db.Database;
import cl.ucn.disc.arqsist.library.service.BookService;
import cl.ucn.disc.arqsist.library.service.LoanService;
import cl.ucn.disc.arqsist.library.service.MemberService;
import cl.ucn.disc.arqsist.library.service.NotFoundException;
import cl.ucn.disc.arqsist.library.service.ReservationService;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;
import io.javalin.json.JavalinJackson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.ZoneId;
import java.util.Map;

/** Wires the database, DAOs, services and controllers, and starts the web server. */
public final class App {

    /** Logger for database initialization, application wiring, and server startup. */
    private static final Logger log = LoggerFactory.getLogger(App.class);

    /** Creates the application entry point; initialization is performed by {@link #main(String[])}. */
    public App() {
    }

    /**
     * Starts the application on port 7070.
     *
     * @param args command-line arguments, not used
     * @throws Exception if the database cannot be opened or seeded
     */
    public static void main(String[] args) throws Exception {
        log.info("Opening database.db");
        Database db = new Database("jdbc:sqlite:database.db");
        log.info("Seeding empty database tables");
        db.seedIfEmpty();

        Clock clock = Clock.system(ZoneId.of("America/Santiago"));
        Javalin app = create(db, clock);
        log.info("Starting the library server on port 7070");
        app.start(7070);
        log.info("Library server started on port {}", app.port());
    }

    /**
     * Wires an unstarted server using the supplied database and clock. Production and HTTP
     * integration tests share this configuration; this method does not seed or close the database.
     *
     * @param db database whose connection source is shared by all DAO adapters
     * @param clock clock used by both lending services
     * @return the configured server, ready to start
     * @throws RuntimeException if DAO creation or server configuration fails
     */
    static Javalin create(Database db, Clock clock) {
        log.info("Creating DAO adapters with a shared database connection source");
        BookDao bookDao = new OrmLiteBookDao(db.connectionSource());
        MemberDao memberDao = new OrmLiteMemberDao(db.connectionSource());
        LoanDao loanDao = new OrmLiteLoanDao(db.connectionSource());
        ReservationDao reservationDao = new OrmLiteReservationDao(db.connectionSource());

        log.info("Creating services with clock zone {}", clock.getZone());
        BookService bookService = new BookService(bookDao);
        MemberService memberService = new MemberService(memberDao);
        LoanService loanService = new LoanService(loanDao, memberDao, bookService, clock);
        ReservationService reservationService = new ReservationService(reservationDao, memberDao, loanDao,
                bookService, clock);

        log.info("Creating service-backed controllers");
        BookController bookController = new BookController(bookService);
        MemberController memberController = new MemberController(memberService);
        LoanController loanController = new LoanController(loanService);
        ReservationController reservationController = new ReservationController(reservationService);

        log.info("Configuring JSON dates, static files, exception handlers, and routes");
        return Javalin.create(config -> {
            config.jsonMapper(new JavalinJackson().updateMapper(mapper ->
                    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)));
            config.staticFiles.add(staticFiles -> {
                staticFiles.hostedPath = "/";
                staticFiles.directory = "/public";
                staticFiles.location = Location.CLASSPATH;
            });
            config.routes.exception(NotFoundException.class, (e, ctx) ->
                    ctx.status(404).json(Map.of("error", e.getMessage())));
            config.routes.exception(IllegalArgumentException.class, (e, ctx) ->
                    ctx.status(400).json(Map.of("error", e.getMessage())));
            config.routes.exception(IllegalStateException.class, (e, ctx) ->
                    ctx.status(409).json(Map.of("error", e.getMessage())));
            bookController.register(config);
            memberController.register(config);
            loanController.register(config);
            reservationController.register(config);
        });
    }
}
