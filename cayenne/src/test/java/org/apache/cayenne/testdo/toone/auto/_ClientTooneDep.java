package org.apache.cayenne.testdo.toone.auto;

import org.apache.cayenne.PersistentObject;
import org.apache.cayenne.ValueHolder;
import org.apache.cayenne.testdo.toone.ClientTooneMaster;

/**
 * A generated persistent class mapped as "TooneDep" Cayenne entity. It is a good idea to
 * avoid changing this class manually, since it will be overwritten next time code is
 * regenerated. If you need to make any customizations, put them in a subclass.
 */
public abstract class _ClientTooneDep extends PersistentObject {

    public static final String TO_MASTER_PROPERTY = "toMaster";

    protected ValueHolder toMaster;

    public ClientTooneMaster getToMaster() {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "toMaster", true);
        }

        return (ClientTooneMaster) toMaster.getValue();
    }
    public void setToMaster(ClientTooneMaster toMaster) {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "toMaster", true);
        }

        this.toMaster.setValue(toMaster);
    }

}
