package org.apache.cayenne.modeler.project;

import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class DbRelationshipOps {

    public static Collection<ObjRelationship> objRelationshipsUsingDbRelationship(
            DataChannelDescriptor domain,
            DbRelationship relationship) {
        List<ObjRelationship> objRelationships = new ArrayList<>();
        if (domain != null) {
            for (DataMap map : domain.getDataMaps()) {
                for (ObjEntity entity : map.getObjEntities()) {
                    for (ObjRelationship objRelationship : entity.getRelationships()) {
                        if (objRelationship.getDbRelationships().contains(relationship)) {
                            objRelationships.add(objRelationship);
                        }
                    }
                }
            }
        }
        return objRelationships;
    }

    public static Collection<ObjAttribute> objAttributesUsingDbRelationship(
            DataChannelDescriptor domain,
            DbRelationship relationship) {

        List<ObjAttribute> attributes = new ArrayList<>();
        if (domain != null) {
            for (DataMap map : domain.getDataMaps()) {
                for (ObjEntity entity : map.getObjEntities()) {
                    for (ObjAttribute objAttribute : entity.getAttributes()) {
                        if (objAttribute.isFlattened()) {
                            objAttribute.getDbPathIterator().forEachRemaining(entry -> {
                                if (entry instanceof DbRelationship) {
                                    if (entry.equals(relationship)) {
                                        attributes.add(objAttribute);
                                    }
                                }
                            });
                        }
                    }
                }
            }
        }
        return attributes;
    }
}
