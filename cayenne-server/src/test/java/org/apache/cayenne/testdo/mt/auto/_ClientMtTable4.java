package org.apache.cayenne.testdo.mt.auto;

import java.util.List;

import org.apache.cayenne.PersistentObject;
import org.apache.cayenne.testdo.mt.ClientMtTable5;

/**
 * A generated persistent class mapped as "MtTable4" Cayenne entity. It is a good idea to
 * avoid changing this class manually, since it will be overwritten next time code is
 * regenerated. If you need to make any customizations, put them in a subclass.
 */
public abstract class _ClientMtTable4 extends PersistentObject {

    public static final String TABLE5S_PROPERTY = "table5s";

    protected List<ClientMtTable5> table5s;

    public List<ClientMtTable5> getTable5s() {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "table5s", true);
        }

        return table5s;
    }
    public void addToTable5s(ClientMtTable5 object) {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "table5s", true);
        }

        this.table5s.add(object);
    }
    public void removeFromTable5s(ClientMtTable5 object) {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "table5s", true);
        }

        this.table5s.remove(object);
    }

}
