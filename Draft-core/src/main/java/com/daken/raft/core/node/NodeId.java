package com.daken.raft.core.node;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.util.Objects;

/**
 * node id
 */
@Getter
@Setter
public class NodeId implements Serializable {

    private final String value;

    public NodeId(@NonNull String value) {
        Objects.requireNonNull(value);
        this.value = value;
    }


    public static NodeId of(@Nonnull String value) {
        return new NodeId(value);
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NodeId nodeId = (NodeId) o;
        return Objects.equals(value, nodeId.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
