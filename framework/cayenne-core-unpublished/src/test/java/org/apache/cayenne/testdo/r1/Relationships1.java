package org.apache.cayenne.testdo.r1;

import org.apache.cayenne.testdo.r1.auto._Relationships1;

public class Relationships1 extends _Relationships1 {

    private static Relationships1 instance;

    private Relationships1() {}

    public static Relationships1 getInstance() {
        if(instance == null) {
            instance = new Relationships1();
        }

        return instance;
    }
}
