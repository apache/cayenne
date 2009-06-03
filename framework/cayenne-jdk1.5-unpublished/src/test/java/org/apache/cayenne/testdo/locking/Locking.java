package org.apache.cayenne.testdo.locking;

import org.apache.cayenne.testdo.locking.auto._Locking;

public class Locking extends _Locking {

    private static Locking instance;

    private Locking() {}

    public static Locking getInstance() {
        if(instance == null) {
            instance = new Locking();
        }

        return instance;
    }
}
