package org.apache.cayenne.testdo.mt.auto;

import org.apache.cayenne.PersistentObject;
import org.apache.cayenne.ValueHolder;
import org.apache.cayenne.testdo.mt.ClientMtMapToMany;

/**
 * A generated persistent class mapped as "MtMapToManyTarget" Cayenne entity. It is a good idea to
 * avoid changing this class manually, since it will be overwritten next time code is
 * regenerated. If you need to make any customizations, put them in a subclass.
 */
public abstract class _ClientMtMapToManyTarget extends PersistentObject {

    public static final String MAP_TO_MANY_PROPERTY = "mapToMany";

    protected ValueHolder mapToMany;

    public ClientMtMapToMany getMapToMany() {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "mapToMany", true);
        }

        return (ClientMtMapToMany) mapToMany.getValue();
    }
    public void setMapToMany(ClientMtMapToMany mapToMany) {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "mapToMany", true);
        }

        this.mapToMany.setValue(mapToMany);
    }

}
