package org.apache.cayenne.testdo.unsupported_distinct_types;

import org.apache.cayenne.testdo.unsupported_distinct_types.auto._UnsupportedDistinctTypes;

public class UnsupportedDistinctTypes extends _UnsupportedDistinctTypes {

    private static UnsupportedDistinctTypes instance;

    private UnsupportedDistinctTypes() {}

    public static UnsupportedDistinctTypes getInstance() {
        if(instance == null) {
            instance = new UnsupportedDistinctTypes();
        }

        return instance;
    }
}
