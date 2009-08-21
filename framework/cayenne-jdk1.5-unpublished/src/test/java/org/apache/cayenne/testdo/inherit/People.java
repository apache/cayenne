package org.apache.cayenne.testdo.inherit;

import org.apache.cayenne.testdo.inherit.auto._People;

public class People extends _People {

    private static People instance;

    private People() {}

    public static People getInstance() {
        if(instance == null) {
            instance = new People();
        }

        return instance;
    }
}
