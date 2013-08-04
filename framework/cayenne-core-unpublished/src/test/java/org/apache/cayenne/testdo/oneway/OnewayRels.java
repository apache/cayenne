package org.apache.cayenne.testdo.oneway;

import org.apache.cayenne.testdo.oneway.auto._OnewayRels;

public class OnewayRels extends _OnewayRels {

    private static OnewayRels instance;

    private OnewayRels() {}

    public static OnewayRels getInstance() {
        if(instance == null) {
            instance = new OnewayRels();
        }

        return instance;
    }
}
