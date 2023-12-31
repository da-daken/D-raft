package com.daken.raft.kvstore.client;

import com.daken.raft.core.service.NoAvailableServerException;

public class KVStoreGetCommand implements Command {

    @Override
    public String getName() {
        return "kvstore-get";
    }

    @Override
    public void execute(String arguments, CommandContext context) {
        if (arguments.isEmpty()) {
            throw new IllegalArgumentException("usage: " + getName() + " <key>");
        }

        byte[] valueBytes;
        try {
            valueBytes = context.getClient().get(arguments);
        } catch (NoAvailableServerException e) {
            System.err.println(e.getMessage());
            return;
        }

        if (valueBytes == null || valueBytes.length == 0) {
            System.out.println("null");
        } else {
            System.out.println(new String(valueBytes));
        }
    }

}
