/*
 * Copyright (c) 2026. Arquitectura de Sistemas, DISC, UCN, Antofagasta.
 */
package cl.ucn.disc.arqsist.library.dao;

import cl.ucn.disc.arqsist.library.model.Member;
import java.util.List;

/**
 * DAO contract for the Member entity.
 */
public interface MemberDao extends CrudDao<Member> {
    /**
     * Finds names containing a literal fragment, ignoring ASCII letter case.
     *
     * @param query trimmed name fragment
     * @return matching members ordered by increasing ID
     */
    List<Member> search(String query);
}
