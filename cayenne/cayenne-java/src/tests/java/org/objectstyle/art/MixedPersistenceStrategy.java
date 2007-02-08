package org.objectstyle.art;

import java.util.ArrayList;
import java.util.List;

import org.objectstyle.art.auto._MixedPersistenceStrategy;

public class MixedPersistenceStrategy extends _MixedPersistenceStrategy {

    // a private variable that intentionally mirrors the name of the persistent property
    // to create a "conflict"
    private List details = new ArrayList();
    
    public List getIvarDetails() {
        return details;
    }
}
