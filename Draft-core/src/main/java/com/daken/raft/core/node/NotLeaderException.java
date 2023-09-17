package com.daken.raft.core.node;

import com.google.common.base.Preconditions;
import com.daken.raft.core.node.role.RoleName;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * NotLeaderException
 */
public class NotLeaderException extends RuntimeException {

    private final RoleName roleName;
    private final NodeEndpoint leaderEndpoint;

    /**
     * Create.
     *
     * @param roleName       role name
     * @param leaderEndpoint leader endpoint
     */
    public NotLeaderException(@Nonnull RoleName roleName, @Nullable NodeEndpoint leaderEndpoint) {
        super("not leader");
        Preconditions.checkNotNull(roleName);
        this.roleName = roleName;
        this.leaderEndpoint = leaderEndpoint;
    }

    /**
     * Get role name.
     *
     * @return role name
     */
    @Nonnull
    public RoleName getRoleName() {
        return roleName;
    }

    /**
     * Get leader endpoint.
     *
     * @return leader endpoint
     */
    @Nullable
    public NodeEndpoint getLeaderEndpoint() {
        return leaderEndpoint;
    }
}
