package org.apache.cayenne.testdo.inheritance.vertical;

import org.apache.cayenne.testdo.inheritance.vertical.auto._InheritanceVertical;

public class InheritanceVertical extends _InheritanceVertical {

    private static InheritanceVertical instance;

    private InheritanceVertical() {}

    public static InheritanceVertical getInstance() {
        if(instance == null) {
            instance = new InheritanceVertical();
        }

        return instance;
    }
}
