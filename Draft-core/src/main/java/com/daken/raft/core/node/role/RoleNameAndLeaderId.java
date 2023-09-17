package com.daken.raft.core.node.role;

import com.daken.raft.core.node.NodeId;
import lombok.Getter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

/**
 * RoleNameAndLeaderId
 */
@Getter
public class RoleNameAndLeaderId {

    private final RoleName roleName;
    private final NodeId leaderId;

    /**
     * Create.
     *
     * @param roleName role name
     * @param leaderId leader id
     */
    public RoleNameAndLeaderId(@Nonnull RoleName roleName, @Nullable NodeId leaderId) {
        Objects.requireNonNull(roleName);
        this.roleName = roleName;
        this.leaderId = leaderId;
    }

}
