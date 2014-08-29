package org.apache.cayenne.access;

import org.apache.cayenne.CayenneException;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.ObjEntity;

/**
 * @since 3.2.
 */
public class DefaultDbLoaderDelegate implements DbLoaderDelegate {

    @Override
    public boolean overwriteDbEntity(DbEntity entity) throws CayenneException {
        return false;
    }

    @Override
    public void dbEntityAdded(DbEntity entity) {

    }

    @Override
    public void dbEntityRemoved(DbEntity entity) {

    }

    @Override
    public void objEntityAdded(ObjEntity entity) {

    }

    @Override
    public void objEntityRemoved(ObjEntity entity) {

    }
}
