package org.apache.cayenne.tutorial.persistent;

import org.apache.cayenne.tutorial.persistent.auto._Datamap;

public class Datamap extends _Datamap {

    private static Datamap instance;

    private Datamap() {}

    public static Datamap getInstance() {
        if(instance == null) {
            instance = new Datamap();
        }

        return instance;
    }
}
