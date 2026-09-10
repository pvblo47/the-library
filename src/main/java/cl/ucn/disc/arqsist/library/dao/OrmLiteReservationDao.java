package cl.ucn.disc.arqsist.library.dao;

import cl.ucn.disc.arqsist.library.model.Reservation;
import com.j256.ormlite.support.ConnectionSource;

/**
 * ORMLite adapter for Reservation persistence.
 */
public final class OrmLiteReservationDao extends BaseDao<Reservation> implements ReservationDao {

    public OrmLiteReservationDao(ConnectionSource connectionSource) {
        super(connectionSource, Reservation.class);
    }
}
