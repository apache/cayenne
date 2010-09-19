package org.apache.cayenne.testdo.consttest;

import org.apache.cayenne.testdo.consttest.auto._Const;

public class Const extends _Const {

    private static Const instance;

    private Const() {}

    public static Const getInstance() {
        if(instance == null) {
            instance = new Const();
        }

        return instance;
    }
}
