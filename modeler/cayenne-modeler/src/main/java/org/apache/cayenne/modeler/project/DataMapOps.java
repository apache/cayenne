package org.apache.cayenne.modeler.project;

import org.apache.cayenne.map.DataMap;

public class DataMapOps {

    /**
     * Cleans any mappings of ObjEntities, ObjAttributes, ObjRelationship to the corresponding Db* objects that no longer exist.
     */
    public static void removeBrokenObjToDbMappings(DataMap map) {
        map.getObjEntities().forEach(oe -> ObjEntityOps.removeBrokenObjToDbMappings(map, oe));
    }
}
