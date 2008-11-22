package org.apache.cayenne.testdo.mt.auto;

import java.util.List;

import org.apache.cayenne.PersistentObject;
import org.apache.cayenne.testdo.mt.ClientMtTable4;

/**
 * A generated persistent class mapped as "MtTable5" Cayenne entity. It is a good idea to
 * avoid changing this class manually, since it will be overwritten next time code is
 * regenerated. If you need to make any customizations, put them in a subclass.
 */
public abstract class _ClientMtTable5 extends PersistentObject {

    public static final String TABLE4S_PROPERTY = "table4s";

    protected List<ClientMtTable4> table4s;

    public List<ClientMtTable4> getTable4s() {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "table4s", true);
        }

        return table4s;
    }
    public void addToTable4s(ClientMtTable4 object) {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "table4s", true);
        }

        this.table4s.add(object);
    }
    public void removeFromTable4s(ClientMtTable4 object) {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "table4s", true);
        }

        this.table4s.remove(object);
    }

}
