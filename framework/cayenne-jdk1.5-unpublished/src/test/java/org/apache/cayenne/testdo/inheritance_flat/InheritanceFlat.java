package org.apache.cayenne.testdo.inheritance_flat;

import org.apache.cayenne.testdo.inheritance_flat.auto._InheritanceFlat;

public class InheritanceFlat extends _InheritanceFlat {

    private static InheritanceFlat instance;

    private InheritanceFlat() {}

    public static InheritanceFlat getInstance() {
        if(instance == null) {
            instance = new InheritanceFlat();
        }

        return instance;
    }
}
