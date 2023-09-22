package com.daken.raft.core.service;

public interface Channel {

    Object send(Object payload);

}
