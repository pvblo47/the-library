/*
 * Copyright (c) 2026. Arquitectura de Sistemas, DISC, UCN, Antofagasta.
 */
package cl.ucn.disc.arqsist.library;

import cl.ucn.disc.arqsist.library.dao.BookDao;
import cl.ucn.disc.arqsist.library.dao.LoanDao;
import cl.ucn.disc.arqsist.library.dao.MemberDao;
import cl.ucn.disc.arqsist.library.dao.ReservationDao;
import cl.ucn.disc.arqsist.library.dao.OrmLiteBookDao;
import cl.ucn.disc.arqsist.library.dao.OrmLiteLoanDao;
import cl.ucn.disc.arqsist.library.dao.OrmLiteMemberDao;
import cl.ucn.disc.arqsist.library.dao.OrmLiteReservationDao;
import cl.ucn.disc.arqsist.library.db.Database;
import cl.ucn.disc.arqsist.library.model.Book;
import cl.ucn.disc.arqsist.library.model.Loan;
import cl.ucn.disc.arqsist.library.model.Member;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.j256.ormlite.dao.DaoManager;
import io.javalin.Javalin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Exercises the real application routes and exception handlers through loopback HTTP. */
class AppHttpIntegrationTest {

    /** Clock that makes all HTTP loan and reservation dates deterministic. */
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-01-15T10:00:00Z"), ZoneOffset.UTC);
    /** Parser used to inspect response JSON without duplicating the server's date mapper. */
    private final ObjectMapper mapper = new ObjectMapper();
    /** In-memory database, never the user's on-disk database. */
    private Database database;
    /** Catalog persistence used for independent state verification. */
    private BookDao bookDao;
    /** Loan persistence used for independent state verification. */
    private LoanDao loanDao;
    /** Reservation persistence used for independent state verification. */
    private ReservationDao reservationDao;
    /** Application server configured by App and bound to a dynamically allocated local port. */
    private Javalin app;
    /** JDK HTTP client used for actual network requests. */
    private HttpClient client;
    /** Persisted book initially containing two available copies. */
    private Book book;
    /** Borrower whose ID deliberately differs from the book ID. */
    private Member member;

    /**
     * Creates isolated data and starts the same server configuration used by main.
     *
     * @throws Exception if database setup or server startup fails
     */
    @BeforeEach
    void setUp() throws Exception {
        database = new Database("jdbc:sqlite::memory:");
        bookDao = new OrmLiteBookDao(database.connectionSource());
        loanDao = new OrmLiteLoanDao(database.connectionSource());
        reservationDao = new OrmLiteReservationDao(database.connectionSource());
        MemberDao memberDao = new OrmLiteMemberDao(database.connectionSource());
        book = new Book("Refactoring", "Martin Fowler", "9780134757599", 2);
        bookDao.create(book);
        memberDao.create(new Member("Test Librarian", "librarian@example.com"));
        member = new Member("Grace Hopper", "grace@example.com");
        memberDao.create(member);
        assertNotEquals(book.getId(), member.getId(), "IDs must differ to catch swapped controller arguments");
        client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        app = App.create(database, CLOCK);
        app.start("127.0.0.1", 0);
    }

    /**
     * Stops the HTTP server and client before closing the database.
     *
     * @throws Exception if database cleanup fails
     */
    @AfterEach
    void tearDown() throws Exception {
        try {
            if (app != null) {
                app.stop();
            }
        } finally {
            try {
                if (client != null) {
                    client.close();
                }
            } finally {
                if (database != null) {
                    database.connectionSource().close();
                }
            }
        }
    }

    /**
     * Catalog and membership routes retain their successful list and create formats.
     *
     * @throws Exception if an HTTP request or JSON parsing fails
     */
    @Test
    void catalogAndMemberRoutesKeepTheirResponses() throws Exception {
        JsonNode books = json(request("GET", "/books", null), 200);
        assertTrue(books.isArray());
        assertEquals(1, books.size());
        assertEquals(book.getId(), books.get(0).get("id").asInt());
        assertEquals("Refactoring", books.get(0).get("title").asText());
        assertEquals(book.getId(), json(request("GET", "/books/" + book.getId(), null), 200).get("id").asInt());

        JsonNode createdBook = json(request("POST", "/books",
                "{\"title\":\"HTTP Book\",\"author\":\"Test Author\",\"isbn\":\"test-isbn\",\"totalCopies\":3}"), 200);
        assertEquals(3, createdBook.get("availableCopies").asInt());
        assertEquals(2, json(request("GET", "/books", null), 200).size());

        assertEquals(2, json(request("GET", "/members", null), 200).size());
        JsonNode createdMember = json(request("POST", "/members",
                "{\"name\":\"Ada Lovelace\",\"email\":\"ada@example.com\"}"), 200);
        assertEquals("Ada Lovelace", createdMember.get("name").asText());
        assertEquals(3, json(request("GET", "/members", null), 200).size());
    }

    /**
     * Loan creation, listing, and return serialize dates as ISO strings and persist inventory.
     *
     * @throws Exception if an HTTP request or persistence verification fails
     */
    @Test
    void checkoutAndReturnUseIsoDatesAndPersistInventory() throws Exception {
        JsonNode loan = json(request("POST", loanPath(), null), 200);
        int loanId = loan.get("id").asInt();
        assertTrue(loan.get("loanDate").isTextual());
        assertEquals("2026-01-15", loan.get("loanDate").asText());
        assertEquals("2026-02-05", loan.get("dueDate").asText());
        assertFalse(loan.has("overdue"));
        assertState(1, 1, 0);
        assertEquals(member.getId(), loanDao.findById(loanId).getMember().getId());
        assertEquals("2026-02-05", json(request("GET", "/loans", null), 200).get(0).get("dueDate").asText());
        assertTrue(json(request("GET", "/loans/overdue", null), 200).isEmpty());

        JsonNode returned = json(request("POST", "/loans/" + loanId + "/return", null), 200);
        assertTrue(returned.get("returned").asBoolean());
        assertEquals("2026-01-15", returned.get("returnDate").asText());
        assertEquals(LocalDate.of(2026, 1, 15), loanDao.findById(loanId).getReturnDate());
        assertState(2, 1, 0);
    }

    /**
     * Both loan-list endpoints expose the same accrued fee without persisting it or changing stock.
     * The overdue endpoint excludes active, due-today, and returned loans.
     *
     * @throws Exception if HTTP or persistence verification fails
     */
    @Test
    void overdueListingUsesTheSameAccruedFeesWithoutPersistingThem() throws Exception {
        LocalDate today = LocalDate.of(2026, 1, 15);
        book.setTotalCopies(4);
        book.setAvailableCopies(1);
        bookDao.update(book);
        Loan overdue = new Loan(member, book, LocalDate.of(2025, 12, 1), LocalDate.of(2025, 12, 22));
        Loan active = new Loan(member, book, today, today.plusDays(21));
        Loan dueToday = new Loan(member, book, today.minusDays(21), today);
        Loan returned = new Loan(member, book, today.minusDays(40), today.minusDays(19));
        returned.setReturned(true);
        returned.setReturnDate(today.minusDays(12));
        returned.setOverdueFee(7.0);
        loanDao.create(overdue);
        loanDao.create(active);
        loanDao.create(dueToday);
        loanDao.create(returned);

        JsonNode overdueList = json(request("GET", "/loans/overdue", null), 200);
        assertEquals(1, overdueList.size());
        assertEquals(overdue.getId(), overdueList.get(0).get("id").asInt());
        assertEquals(24.0, overdueList.get(0).get("overdueFee").asDouble());
        assertEquals(0.0, loanDao.findById(overdue.getId()).getOverdueFee());

        JsonNode allLoans = json(request("GET", "/loans", null), 200);
        Map<Integer, Double> expectedFees = Map.of(overdue.getId(), 24.0, active.getId(), 0.0,
                dueToday.getId(), 0.0, returned.getId(), 7.0);
        assertEquals(4, allLoans.size());
        for (JsonNode loan : allLoans) {
            int id = loan.get("id").asInt();
            assertTrue(expectedFees.containsKey(id));
            assertEquals(expectedFees.get(id).doubleValue(), loan.get("overdueFee").asDouble());
            if (id == overdue.getId()) {
                assertEquals(loan.get("overdueFee").asDouble(), overdueList.get(0).get("overdueFee").asDouble());
            }
        }
        assertEquals(0.0, loanDao.findById(overdue.getId()).getOverdueFee());
        assertEquals(0.0, loanDao.findById(active.getId()).getOverdueFee());
        assertEquals(0.0, loanDao.findById(dueToday.getId()).getOverdueFee());
        assertEquals(7.0, loanDao.findById(returned.getId()).getOverdueFee());
        assertFalse(loanDao.findById(overdue.getId()).isReturned());
        assertNull(loanDao.findById(overdue.getId()).getReturnDate());
        assertState(1, 4, 0);
    }

    /**
     * Reservation routes preserve member/book argument order and fulfill through the service.
     *
     * @throws Exception if HTTP or persistence verification fails
     */
    @Test
    void reservationCreationAndFulfillmentUseCorrectIdsAndIsoDates() throws Exception {
        JsonNode reservation = json(request("POST", reservationPath(), null), 200);
        int reservationId = reservation.get("id").asInt();
        assertEquals("2026-01-15", reservation.get("reservedAt").asText());
        assertEquals(member.getId(), reservation.get("member").get("id").asInt());
        assertEquals(book.getId(), reservation.get("book").get("id").asInt());
        assertState(2, 0, 1);
        assertFalse(reservationDao.findById(reservationId).isFulfilled());
        assertEquals(reservationId, json(request("GET", "/reservations", null), 200).get(0).get("id").asInt());

        JsonNode loan = json(request("POST", "/reservations/" + reservationId + "/fulfill", null), 200);
        assertEquals("2026-01-15", loan.get("loanDate").asText());
        assertEquals("2026-02-05", loan.get("dueDate").asText());
        assertEquals(member.getId(), loanDao.findById(loan.get("id").asInt()).getMember().getId());
        assertTrue(reservationDao.findById(reservationId).isFulfilled());
        assertState(1, 1, 1);
    }

    /**
     * Missing, malformed, and out-of-range numbers produce JSON HTTP 400 without writes.
     *
     * @throws Exception if an HTTP request or JSON parsing fails
     */
    @Test
    void invalidNumericParametersProduce400WithoutWrites() throws Exception {
        for (String route : new String[]{"/loans", "/reservations"}) {
            for (String query : new String[]{"", "?memberId=" + member.getId(), "?bookId=" + book.getId(),
                    "?memberId=invalid&bookId=" + book.getId(),
                    "?memberId=" + member.getId() + "&bookId=invalid",
                    "?memberId=999999999999999999999&bookId=" + book.getId()}) {
                assertError(request("POST", route + query, null), 400, null);
            }
        }
        assertError(request("GET", "/books/invalid", null), 400, null);
        assertError(request("POST", "/loans/invalid/return", null), 400, null);
        assertError(request("POST", "/reservations/invalid/fulfill", null), 400, null);
        assertState(2, 0, 0);
    }

    /**
     * Missing lending entities reach the JSON HTTP 404 handler without partial writes.
     *
     * @throws Exception if an HTTP request or JSON parsing fails
     */
    @Test
    void missingEntitiesProduce404WithoutWrites() throws Exception {
        assertError(request("POST", "/loans?memberId=9999&bookId=" + book.getId(), null),
                404, "Member not found: 9999");
        assertError(request("POST", "/loans?memberId=" + member.getId() + "&bookId=9999", null),
                404, "Book not found: 9999");
        assertError(request("POST", "/loans/9999/return", null), 404, "Loan not found: 9999");
        assertError(request("POST", "/reservations?memberId=9999&bookId=" + book.getId(), null),
                404, "Member not found: 9999");
        assertError(request("POST", "/reservations?memberId=" + member.getId() + "&bookId=9999", null),
                404, "Book not found: 9999");
        assertError(request("POST", "/reservations/9999/fulfill", null), 404, "Reservation not found: 9999");
        assertState(2, 0, 0);
    }

    /**
     * Repeated returns and exhausted stock produce HTTP 409 with consistent stored state.
     *
     * @throws Exception if HTTP or persistence verification fails
     */
    @Test
    void loanConflictsProduce409WithoutExtraInventoryChanges() throws Exception {
        int id = json(request("POST", loanPath(), null), 200).get("id").asInt();
        json(request("POST", "/loans/" + id + "/return", null), 200);
        assertError(request("POST", "/loans/" + id + "/return", null), 409, "Loan already returned: " + id);
        assertTrue(loanDao.findById(id).isReturned());
        assertState(2, 1, 0);

        book.setAvailableCopies(0);
        bookDao.update(book);
        assertError(request("POST", loanPath(), null), 409, "No available copies of book " + book.getId());
        assertState(0, 1, 0);
    }

    /**
     * Failed and repeated fulfillment return HTTP 409 without duplicating loans or stock changes.
     *
     * @throws Exception if HTTP or persistence verification fails
     */
    @Test
    void reservationConflictsProduce409AndRollbackTheFulfilledFlag() throws Exception {
        int id = json(request("POST", reservationPath(), null), 200).get("id").asInt();
        book.setAvailableCopies(0);
        bookDao.update(book);
        assertError(request("POST", "/reservations/" + id + "/fulfill", null),
                409, "No available copies of book " + book.getId());
        assertFalse(reservationDao.findById(id).isFulfilled());
        assertState(0, 0, 1);

        book.setAvailableCopies(1);
        bookDao.update(book);
        json(request("POST", "/reservations/" + id + "/fulfill", null), 200);
        assertError(request("POST", "/reservations/" + id + "/fulfill", null),
                409, "Reservation already fulfilled: " + id);
        assertTrue(reservationDao.findById(id).isFulfilled());
        assertState(0, 1, 1);
    }

    /**
     * An actual database failure reached through HTTP rolls back a preceding inventory write.
     *
     * @throws Exception if HTTP, trigger setup, or persistence verification fails
     */
    @Test
    void failedLoanInsertOverHttpRollsBackInventory() throws Exception {
        executeSql("CREATE TRIGGER fail_http_loan AFTER INSERT ON loans "
                + "BEGIN SELECT RAISE(ABORT, 'injected HTTP loan failure'); END");
        assertEquals(500, request("POST", loanPath(), null).statusCode());
        assertState(2, 0, 0);
        executeSql("DROP TRIGGER fail_http_loan");
        json(request("POST", loanPath(), null), 200);
        assertState(1, 1, 0);
    }

    /**
     * A loan insert failure over HTTP restores both the reservation flag and inventory.
     *
     * @throws Exception if HTTP, trigger setup, or persistence verification fails
     */
    @Test
    void failedFulfillmentOverHttpRollsBackReservationAndInventory() throws Exception {
        int id = json(request("POST", reservationPath(), null), 200).get("id").asInt();
        executeSql("CREATE TRIGGER fail_http_fulfillment AFTER INSERT ON loans "
                + "BEGIN SELECT RAISE(ABORT, 'injected HTTP fulfillment failure'); END");
        assertEquals(500, request("POST", "/reservations/" + id + "/fulfill", null).statusCode());
        assertFalse(reservationDao.findById(id).isFulfilled());
        assertState(2, 0, 1);
        executeSql("DROP TRIGGER fail_http_fulfillment");
        json(request("POST", "/reservations/" + id + "/fulfill", null), 200);
        assertTrue(reservationDao.findById(id).isFulfilled());
        assertState(1, 1, 1);
    }

    /**
     * The unchanged classpath static-file configuration still serves the browser entry point.
     *
     * @throws Exception if the HTTP request fails
     */
    @Test
    void staticBrowserEntryPointIsServed() throws Exception {
        HttpResponse<String> response = request("GET", "/", null);
        assertEquals(200, response.statusCode());
        assertTrue(response.headers().firstValue("Content-Type").orElse("").contains("text/html"));
        assertTrue(response.body().contains("<html"));
    }

    /**
     * Sends a real loopback HTTP request to the running application.
     *
     * @param method HTTP method
     * @param path relative route and query string
     * @param body JSON request body, or null for an empty body
     * @return complete HTTP response
     * @throws Exception if the request fails or times out
     */
    private HttpResponse<String> request(String method, String path, String body) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + app.port() + path))
                .timeout(Duration.ofSeconds(10));
        if (body != null) {
            builder.header("Content-Type", "application/json");
        }
        builder.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    /**
     * Checks a response status and parses its JSON body.
     *
     * @param response response to inspect
     * @param expectedStatus required HTTP status
     * @return parsed response JSON
     * @throws Exception if the response cannot be parsed
     */
    private JsonNode json(HttpResponse<String> response, int expectedStatus) throws Exception {
        assertEquals(expectedStatus, response.statusCode(), response.body());
        assertTrue(response.headers().firstValue("Content-Type").orElse("").contains("application/json"));
        return mapper.readTree(response.body());
    }

    /**
     * Checks the error response contains only the expected error message property.
     *
     * @param response response returned by a failing request
     * @param expectedStatus required HTTP error status
     * @param expectedMessage exact message, or null to accept any nonblank parsing error
     * @throws Exception if the response JSON cannot be parsed
     */
    private void assertError(HttpResponse<String> response, int expectedStatus, String expectedMessage) throws Exception {
        JsonNode error = json(response, expectedStatus);
        assertTrue(error.isObject());
        assertEquals(1, error.size());
        assertTrue(error.get("error").isTextual());
        assertFalse(error.get("error").asText().isBlank());
        if (expectedMessage != null) {
            assertEquals(expectedMessage, error.get("error").asText());
        }
    }

    /**
     * Checks database state independently of the response body.
     *
     * @param expectedCopies required available copies for the test book
     * @param expectedLoans required number of stored loans
     * @param expectedReservations required number of stored reservations
     */
    private void assertState(int expectedCopies, int expectedLoans, int expectedReservations) {
        assertEquals(expectedCopies, bookDao.findById(book.getId()).getAvailableCopies());
        assertEquals(expectedLoans, loanDao.findAll().size());
        assertEquals(expectedReservations, reservationDao.findAll().size());
    }

    /**
     * Builds the valid checkout route for the deliberately different member and book IDs.
     *
     * @return loan creation route with query parameters
     */
    private String loanPath() {
        return "/loans?memberId=" + member.getId() + "&bookId=" + book.getId();
    }

    /**
     * Builds the valid reservation route for the deliberately different member and book IDs.
     *
     * @return reservation creation route with query parameters
     */
    private String reservationPath() {
        return "/reservations?memberId=" + member.getId() + "&bookId=" + book.getId();
    }

    /**
     * Installs or removes a failure trigger in the isolated database.
     *
     * @param sql SQLite trigger statement
     * @throws Exception if the statement fails
     */
    private void executeSql(String sql) throws Exception {
        DaoManager.createDao(database.connectionSource(), Loan.class).executeRaw(sql);
    }
}
