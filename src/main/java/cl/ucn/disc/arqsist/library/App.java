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
import cl.ucn.disc.arqsist.library.service.ReservationService;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;
import io.javalin.json.JavalinJackson;

/** Wires the database, DAOs, services and controllers, and starts the web server. */
public final class App {

    /**
     * Starts the application on port 7070.
     *
     * @param args command-line arguments, not used
     * @throws Exception if the database cannot be opened or seeded
     */
    public static void main(String[] args) throws Exception {
        Database db = new Database("jdbc:sqlite:database.sqlite");
        db.seedIfEmpty();

        BookDao bookDao = new OrmLiteBookDao(db.connectionSource());
        MemberDao memberDao = new OrmLiteMemberDao(db.connectionSource());
        LoanDao loanDao = new OrmLiteLoanDao(db.connectionSource());
        ReservationDao reservationDao = new OrmLiteReservationDao(db.connectionSource());

        BookService bookService = new BookService(bookDao);
        MemberService memberService = new MemberService(memberDao, bookDao, loanDao);
        LoanService loanService = new LoanService(loanDao, bookDao);
        ReservationService reservationService = new ReservationService(reservationDao, bookDao, memberDao, loanDao);

        BookController bookController = new BookController(bookService, bookDao);
        MemberController memberController = new MemberController(memberService);
        LoanController loanController = new LoanController(memberService, loanService);
        ReservationController reservationController = new ReservationController(reservationService);

        Javalin.create(config -> {
            config.jsonMapper(new JavalinJackson().updateMapper(mapper ->
                    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)));
            config.staticFiles.add(staticFiles -> {
                staticFiles.hostedPath = "/";
                staticFiles.directory = "/public";
                staticFiles.location = Location.CLASSPATH;
            });
            bookController.register(config);
            memberController.register(config);
            loanController.register(config);
            reservationController.register(config);
        }).start(7070);
    }
}
