/*
 * Copyright (c) 2026. Arquitectura de Sistemas, DISC, UCN, Antofagasta.
 */
package cl.ucn.disc.arqsist.library.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Verifies the loan period across calendar boundaries using fixed dates. */
class LoanPolicyTest {

    /** Verifies that the loan period crosses a year boundary correctly. */
    @Test
    void dueDateCrossesYearBoundary() {
        assertEquals(LocalDate.of(2027, 1, 10),
                LoanPolicy.dueDate(LocalDate.of(2026, 12, 20)));
    }

    /** Verifies that leap day counts as a calendar day in the loan period. */
    @Test
    void dueDateIncludesLeapDay() {
        assertEquals(LocalDate.of(2028, 3, 1),
                LoanPolicy.dueDate(LocalDate.of(2028, 2, 9)));
    }
}
