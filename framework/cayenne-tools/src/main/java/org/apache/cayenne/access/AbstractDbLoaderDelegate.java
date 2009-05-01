package org.apache.cayenne.access;

import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.CayenneException;

import java.util.ArrayList;
import java.util.List;
import java.util.Collections;

public abstract class AbstractDbLoaderDelegate implements DbLoaderDelegate {

    private List<DbEntity> addedDbEntities = new ArrayList<DbEntity>();
    private List<DbEntity> removedDbEntities = new ArrayList<DbEntity>();
    private List<ObjEntity> addedObjEntities = new ArrayList<ObjEntity>();
    private List<ObjEntity> removedObjEntities = new ArrayList<ObjEntity>();

    public boolean overwriteDbEntity(final DbEntity ent) throws CayenneException {
        return false;
    }

    public void dbEntityAdded(final DbEntity ent) {
        ent.getDataMap().addDbEntity(ent);
        addedDbEntities.add(ent);
    }

    public void dbEntityRemoved(final DbEntity ent) {
        ent.getDataMap().removeDbEntity(ent.getName());
        removedDbEntities.add(ent);
    }

    public void objEntityAdded(final ObjEntity ent) {
        ent.getDataMap().addObjEntity(ent);
        addedObjEntities.add(ent);
    }

    public void objEntityRemoved(final ObjEntity ent) {
        ent.getDataMap().removeObjEntity(ent.getName());
        removedObjEntities.add(ent);
    }

    public List<DbEntity> getAddedDbEntities() {
        return Collections.unmodifiableList(addedDbEntities);
    }

    public List<DbEntity> getRemovedDbEntities() {
        return Collections.unmodifiableList(removedDbEntities);
    }

    public List<ObjEntity> getAddedObjEntities() {
        return Collections.unmodifiableList(addedObjEntities);
    }

    public List<ObjEntity> getRemovedObjEntities() {
        return Collections.unmodifiableList(removedObjEntities);
    }
}
