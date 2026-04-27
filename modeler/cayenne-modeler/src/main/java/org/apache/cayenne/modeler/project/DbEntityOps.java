package org.apache.cayenne.modeler.project;

import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;

public class DbEntityOps {


    public static boolean isValidDbPath(DbEntity root, String dbPath) {
        if (root == null) {
            return true;
        }

        String[] pathSplit = dbPath.split("\\.");

        int size = pathSplit.length - 1;
        DbEntity next = root;
        for (int j = 0; j < size; j++) {
            DbRelationship relationship = next.getRelationship(pathSplit[j]);
            if (relationship == null) {
                return false;
            }
            next = relationship.getTargetEntity();
        }

        return next.getAttribute(pathSplit[(size)]) != null;
    }
}
