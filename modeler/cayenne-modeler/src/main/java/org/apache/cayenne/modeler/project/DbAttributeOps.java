package org.apache.cayenne.modeler.project;

import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.DbRelationship;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class DbAttributeOps {

    public static Collection<DbRelationship> relationshipsUsingAttribute(DbAttribute attribute) {
        DbEntity parent = attribute.getEntity();
        if (parent == null) {
            return Collections.emptyList();
        }

        DataMap map = parent.getDataMap();
        Iterable<DbEntity> entities = (map != null) ? map.getDbEntities() : Collections.singleton(parent);

        Collection<DbRelationship> relationships = new ArrayList<>();
        for (DbEntity entity : entities) {
            for (DbRelationship relationship : entity.getRelationships()) {
                for (DbJoin join : relationship.getJoins()) {
                    if (join.getSource() == attribute || join.getTarget() == attribute) {
                        relationships.add(relationship);
                        break;
                    }
                }
            }
        }
        return relationships;
    }
}
