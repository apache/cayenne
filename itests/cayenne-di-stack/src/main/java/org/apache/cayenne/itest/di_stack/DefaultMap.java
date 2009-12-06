package org.apache.cayenne.itest.di_stack;

import org.apache.cayenne.itest.di_stack.auto._DefaultMap;

public class DefaultMap extends _DefaultMap {

    private static DefaultMap instance;

    private DefaultMap() {}

    public static DefaultMap getInstance() {
        if(instance == null) {
            instance = new DefaultMap();
        }

        return instance;
    }
}
