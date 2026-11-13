/*
 * Copyright (c) 2026. Arquitectura de Sistemas, DISC, UCN, Antofagasta.
 */
package cl.ucn.disc.arqsist.library.dao;

import cl.ucn.disc.arqsist.library.model.Member;
import com.j256.ormlite.stmt.SelectArg;
import com.j256.ormlite.support.ConnectionSource;
import java.util.List;

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

    /** {@inheritDoc} */
    @Override
    public List<Member> search(String query) {
        return execute(() -> {
            var builder = dao.queryBuilder();
            builder.where().raw("instr(lower(name), lower(?)) > 0", new SelectArg("name", query));
            return builder.orderBy("id", true).query();
        }, "Error searching members by name");
    }
}
