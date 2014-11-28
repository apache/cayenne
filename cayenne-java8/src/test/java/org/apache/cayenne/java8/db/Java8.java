package org.apache.cayenne.java8.db;

import org.apache.cayenne.java8.db.auto._Java8;

public class Java8 extends _Java8 {

    private static Java8 instance;

    private Java8() {}

    public static Java8 getInstance() {
        if(instance == null) {
            instance = new Java8();
        }

        return instance;
    }
}
