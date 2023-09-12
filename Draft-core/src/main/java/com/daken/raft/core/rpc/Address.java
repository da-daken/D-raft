package com.daken.raft.core.rpc;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Address
 */
@Data
@AllArgsConstructor
public class Address {

    private final String host;
    private final int port;

}
