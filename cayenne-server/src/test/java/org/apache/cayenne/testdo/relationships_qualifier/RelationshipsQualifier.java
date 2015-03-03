package org.apache.cayenne.testdo.relationships_qualifier;

import org.apache.cayenne.testdo.relationships_qualifier.auto._RelationshipsQualifier;

public class RelationshipsQualifier extends _RelationshipsQualifier {

    private static RelationshipsQualifier instance;

    private RelationshipsQualifier() {}

    public static RelationshipsQualifier getInstance() {
        if(instance == null) {
            instance = new RelationshipsQualifier();
        }

        return instance;
    }
}
