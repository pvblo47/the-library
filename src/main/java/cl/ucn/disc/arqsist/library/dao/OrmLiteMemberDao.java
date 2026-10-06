/*
 * Copyright (c) 2026. Arquitectura de Sistemas, DISC, UCN, Antofagasta.
 */
package cl.ucn.disc.arqsist.library.dao;

import cl.ucn.disc.arqsist.library.model.Member;
import com.j256.ormlite.support.ConnectionSource;

/**
 * ORMLite adapter for Member persistence.
 */
public final class OrmLiteMemberDao extends BaseDao<Member> implements MemberDao {

    /**
     * Creates the ORMLite adapter for Member persistence.
     *
     * @param connectionSource connection source of the database that holds the member table
     * @throws RuntimeException if the underlying ORMLite DAO cannot be created
     */
    public OrmLiteMemberDao(ConnectionSource connectionSource) {
        super(connectionSource, Member.class);
    }
}
