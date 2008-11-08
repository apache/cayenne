package org.apache.cayenne.testdo.mt;

import org.apache.cayenne.testdo.mt.auto._MultiTier;

public class MultiTier extends _MultiTier {

    private static MultiTier instance;

    private MultiTier() {}

    public static MultiTier getInstance() {
        if(instance == null) {
            instance = new MultiTier();
        }

        return instance;
    }
}
