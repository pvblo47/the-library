/*
 * Copyright (c) 2026. Arquitectura de Sistemas, DISC, UCN, Antofagasta.
 */
package cl.ucn.disc.arqsist.library.service;

import java.time.LocalDate;

/** Shared loan duration and overdue fee policy. */
public final class LoanPolicy {

    /** Number of calendar days allowed for a loan. */
    public static final int DUE_DAYS = 21;

    /** Fee charged for each overdue day. */
    public static final double FEE_PER_DAY = 1.0;

    /** Prevents instantiation of this utility class. */
    private LoanPolicy() {
    }

    /**
     * Calculates the due date from the date a loan starts.
     *
     * @param loanDate date the loan starts
     * @return date on which the loan is due
     * @throws NullPointerException if loanDate is null
     * @throws java.time.DateTimeException if the result exceeds the supported date range
     */
    public static LocalDate dueDate(LocalDate loanDate) {
        return loanDate.plusDays(DUE_DAYS);
    }
}
