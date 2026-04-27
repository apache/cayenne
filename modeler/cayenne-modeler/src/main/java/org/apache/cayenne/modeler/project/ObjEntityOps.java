package org.apache.cayenne.modeler.project;

import org.apache.cayenne.exp.path.CayennePath;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ObjEntityOps {

    // While we are duplicating EntityInheritanceTree.allSubEntities(..), its cache may not be refreshed in the modeler
    // on every entity operation
    public static Collection<ObjEntity> subentities(ObjEntity root) {
        Collection<ObjEntity> subentities = new ArrayList<>();
        for (ObjEntity child : root.getDataMap().getObjEntities()) {
            if (child.isSubentityOf(root)) {
                subentities.add(child);
            }
        }
        return subentities;
    }

    /**
     * Cleans any mappings of ObjEntities, ObjAttributes, ObjRelationship to the corresponding Db* objects that no longer exist.
     */
    public static void removeBrokenObjToDbMappings(DataMap map, ObjEntity oe) {

        DbEntity de = oe.getDbEntity();

        // the whole entity mapping is invalid
        if (de != null && map.getDbEntity(de.getName()) != de) {
            unmapFromDbEntity(oe);
            return;
        }

        // check individual attributes
        for (ObjAttribute oa : oe.getAttributes()) {

            // If flattened attribute
            String dbAttributePath = oa.getDbAttributePath().value();
            if (dbAttributePath != null && dbAttributePath.contains(".")) {
                String[] pathSplit = dbAttributePath.split("\\.");

                // If flattened attribute
                if (pathSplit.length > 1) {

                    boolean isTruePath = DbEntityOps.isValidDbPath(de, dbAttributePath);

                    if (!isTruePath) {
                        oa.setDbAttributePath((String) null);
                    }
                }
            } else {
                DbAttribute da = oa.getDbAttribute();
                if (da != null) {
                    if (de.getAttribute(da.getName()) != da) {
                        oa.setDbAttributePath((String) null);
                    }
                }
            }
        }

        // check individual relationships
        for (ObjRelationship or : oe.getRelationships()) {

            List<DbRelationship> dbRelList = new ArrayList<>(or.getDbRelationships());
            for (DbRelationship dr : dbRelList) {
                DbEntity srcEnt = dr.getSourceEntity();
                if (srcEnt == null
                        || map.getDbEntity(srcEnt.getName()) != srcEnt
                        || srcEnt.getRelationship(dr.getName()) != dr) {
                    or.removeDbRelationship(dr);
                }
            }
        }
    }

    private static void unmapFromDbEntity(ObjEntity entity) {
        DbEntity de = entity.getDbEntity();
        if (de == null) {
            return;
        }

        for (ObjAttribute oa : entity.getAttributeMap().values()) {
            oa.setDbAttributePath(CayennePath.EMPTY_PATH);
        }

        for (ObjRelationship r : entity.getRelationships()) {
            r.clearDbRelationships();
        }

        // TODO: should "setDbEntity(null)" do all of the above?
        entity.setDbEntity(null);
    }
}
