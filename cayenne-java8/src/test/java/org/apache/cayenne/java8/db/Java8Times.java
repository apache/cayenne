package org.apache.cayenne.java8.db;

import org.apache.cayenne.java8.db.auto._Java8Times;

public class Java8Times extends _Java8Times {

    private static Java8Times instance;

    private Java8Times() {}

    public static Java8Times getInstance() {
        if(instance == null) {
            instance = new Java8Times();
        }

        return instance;
    }
}
