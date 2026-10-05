/*
 * Copyright (c) 2026. Arquitectura de Sistemas, DISC, UCN, Antofagasta.
 */
package cl.ucn.disc.arqsist.library.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests that a {@link Loan} with {@link LocalDate} fields can be written as JSON with ISO date strings. */
class LoanJsonTest {

    /**
     * Dates are written as ISO strings and the removed {@code overdue} property is absent. This fails
     * with the {@code LocalDate} "not supported by default" error when the jsr310 module is missing.
     *
     * @throws Exception if the loan cannot be written as JSON
     */
    @Test
    void loanDatesAreWrittenAsIsoStringsWithoutAnOverdueProperty() throws Exception {
        ObjectMapper mapper = new ObjectMapper()
                .findAndRegisterModules()
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        Loan loan = new Loan(new Member("Ada Lovelace", "ada@example.com"),
                new Book("Clean Code", "Robert C. Martin", "9780132350884", 1),
                LocalDate.of(2026, 1, 15), LocalDate.of(2026, 2, 5));

        String json = mapper.writeValueAsString(loan);

        assertTrue(json.contains("\"loanDate\":\"2026-01-15\""), json);
        assertTrue(json.contains("\"dueDate\":\"2026-02-05\""), json);
        assertFalse(json.contains("\"overdue\":"), json);
    }
}
