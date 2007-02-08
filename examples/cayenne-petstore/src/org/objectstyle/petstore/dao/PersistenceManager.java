package org.objectstyle.petstore.dao;

import java.util.List;

public interface PersistenceManager {

    void persistObject(Object object);

    Object findObject(Class objectClass, Object pk);

    List getAllObjects(Class objectClass);

    void commitChanges();
}
