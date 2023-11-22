package org.apache.cayenne.testdo.map_to_many.auto;

import org.apache.cayenne.PersistentObject;
import org.apache.cayenne.ValueHolder;
import org.apache.cayenne.testdo.map_to_many.ClientIdMapToMany;

/**
 * A generated persistent class mapped as "IdMapToManyTarget" Cayenne entity. It is a good idea to
 * avoid changing this class manually, since it will be overwritten next time code is
 * regenerated. If you need to make any customizations, put them in a subclass.
 */
public abstract class _ClientIdMapToManyTarget extends PersistentObject {

    public static final String MAP_TO_MANY_PROPERTY = "mapToMany";

    protected ValueHolder mapToMany;

    public ClientIdMapToMany getMapToMany() {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "mapToMany", true);
        }

        return (ClientIdMapToMany) mapToMany.getValue();
    }
    public void setMapToMany(ClientIdMapToMany mapToMany) {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "mapToMany", true);
        }

        this.mapToMany.setValue(mapToMany);
    }

}
