package cl.ucn.disc.arqsist.library.dao;

import cl.ucn.disc.arqsist.library.model.Member;
import com.j256.ormlite.support.ConnectionSource;

/**
 * ORMLite adapter for Member persistence.
 */
public final class OrmLiteMemberDao extends BaseDao<Member> implements MemberDao {

    public OrmLiteMemberDao(ConnectionSource connectionSource) {
        super(connectionSource, Member.class);
    }
}
