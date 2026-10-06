/*
 * Copyright (c) 2026. Arquitectura de Sistemas, DISC, UCN, Antofagasta.
 */
package cl.ucn.disc.arqsist.library.dao;

import cl.ucn.disc.arqsist.library.model.Reservation;
import com.j256.ormlite.support.ConnectionSource;

/**
 * ORMLite adapter for Reservation persistence.
 */
public final class OrmLiteReservationDao extends BaseDao<Reservation> implements ReservationDao {

    /**
     * Creates the ORMLite adapter for Reservation persistence.
     *
     * @param connectionSource connection source of the database that holds the reservation table
     * @throws RuntimeException if the underlying ORMLite DAO cannot be created
     */
    public OrmLiteReservationDao(ConnectionSource connectionSource) {
        super(connectionSource, Reservation.class);
    }
}
