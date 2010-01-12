package org.apache.cayenne.testdo.relationship;

import org.apache.cayenne.testdo.relationship.auto._Relationships;

public class Relationships extends _Relationships {

    private static Relationships instance;

    private Relationships() {}

    public static Relationships getInstance() {
        if(instance == null) {
            instance = new Relationships();
        }

        return instance;
    }
}
