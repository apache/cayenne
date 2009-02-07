package org.apache.cayenne.testdo.quotemap;

import org.apache.cayenne.testdo.quotemap.auto._Quotemap;

public class Quotemap extends _Quotemap {

    private static Quotemap instance;

    private Quotemap() {}

    public static Quotemap getInstance() {
        if(instance == null) {
            instance = new Quotemap();
        }

        return instance;
    }
}
