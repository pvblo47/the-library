package cl.ucn.disc.arqsist.library.dao;

import cl.ucn.disc.arqsist.library.model.Loan;
import com.j256.ormlite.support.ConnectionSource;

/**
 * ORMLite adapter for Loan persistence.
 */
public final class OrmLiteLoanDao extends BaseDao<Loan> implements LoanDao {

    public OrmLiteLoanDao(ConnectionSource connectionSource) {
        super(connectionSource, Loan.class);
    }
}
