package org.apache.cayenne.lifecycle.db;

import org.apache.cayenne.lifecycle.db.auto._LifecycleMap;

public class LifecycleMap extends _LifecycleMap {

    private static LifecycleMap instance;

    private LifecycleMap() {}

    public static LifecycleMap getInstance() {
        if(instance == null) {
            instance = new LifecycleMap();
        }

        return instance;
    }
}
