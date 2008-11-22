package org.apache.cayenne.testdo.generic;

import org.apache.cayenne.testdo.generic.auto._Generic;

public class Generic extends _Generic {

    private static Generic instance;

    private Generic() {}

    public static Generic getInstance() {
        if(instance == null) {
            instance = new Generic();
        }

        return instance;
    }
}
