/*
 * Copyright (c) 2026. Arquitectura de Sistemas, DISC, UCN, Antofagasta.
 */
package cl.ucn.disc.arqsist.library.dao;

import cl.ucn.disc.arqsist.library.model.Loan;
import com.j256.ormlite.support.ConnectionSource;

/**
 * ORMLite adapter for Loan persistence.
 */
public final class OrmLiteLoanDao extends BaseDao<Loan> implements LoanDao {

    /**
     * Creates the ORMLite adapter for Loan persistence.
     *
     * @param connectionSource connection source of the database that holds the loan table
     * @throws RuntimeException if the underlying ORMLite DAO cannot be created
     */
    public OrmLiteLoanDao(ConnectionSource connectionSource) {
        super(connectionSource, Loan.class);
    }
}
