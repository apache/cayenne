package org.apache.cayenne.tutorial.persistent;

import org.apache.cayenne.tutorial.persistent.auto._UntitledDomainMap;

public class UntitledDomainMap extends _UntitledDomainMap {

    private static UntitledDomainMap instance;

    private UntitledDomainMap() {}

    public static UntitledDomainMap getInstance() {
        if(instance == null) {
            instance = new UntitledDomainMap();
        }

        return instance;
    }
}
