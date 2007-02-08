package org.objectstyle.petstore.dao.impl;

import java.util.List;

import org.objectstyle.cayenne.DataObject;
import org.objectstyle.cayenne.DataObjectUtils;
import org.objectstyle.cayenne.query.SelectQuery;
import org.objectstyle.petstore.dao.PersistenceManager;

public class CayennePersistenceManager extends CayenneDao implements PersistenceManager {

    public void persistObject(Object object) {
        if (!(object instanceof DataObject)) {
            throw new IllegalArgumentException("Expected DataObject, got: " + object);
        }

        getDataContext().registerNewObject((DataObject) object);
    }

    public Object findObject(Class objectClass, Object pk) {
        if (pk == null) {
            return null;
        }
        
        return DataObjectUtils.objectForPK(getDataContext(), objectClass, pk);
    }

    public List getAllObjects(Class objectClass) {
        return getDataContext().performQuery(new SelectQuery(objectClass));
    }

    public void commitChanges() {
        getDataContext().commitChanges();
    }
}
