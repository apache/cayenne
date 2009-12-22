package org.example.cayenne.persistent;

import org.example.cayenne.persistent.auto._UntitledDomainMap;

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
