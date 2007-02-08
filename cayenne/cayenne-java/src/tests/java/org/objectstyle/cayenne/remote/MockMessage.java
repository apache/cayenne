package org.objectstyle.cayenne.remote;

import org.objectstyle.cayenne.DataChannel;
import org.objectstyle.cayenne.remote.ClientMessage;

public class MockMessage implements ClientMessage {

    DataChannel lastChannel;

    public MockMessage() {

    }

    public Object dispatch(DataChannel channel) {
        this.lastChannel = channel;
        return null;
    }
    
    public DataChannel getLastChannel() {
        return lastChannel;
    }
} 
