package org.apache.cayenne.testdo.cay_2641;

import org.apache.cayenne.testdo.cay_2641.auto._DatamapLazy;

public class DatamapLazy extends _DatamapLazy {

    private static DatamapLazy instance;

    private DatamapLazy() {}

    public static DatamapLazy getInstance() {
        if(instance == null) {
            instance = new DatamapLazy();
        }

        return instance;
    }
}
