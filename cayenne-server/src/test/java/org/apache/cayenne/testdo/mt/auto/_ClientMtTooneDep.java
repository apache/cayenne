package org.apache.cayenne.testdo.mt.auto;

import org.apache.cayenne.PersistentObject;
import org.apache.cayenne.ValueHolder;
import org.apache.cayenne.testdo.mt.ClientMtTooneMaster;

/**
 * A generated persistent class mapped as "MtTooneDep" Cayenne entity. It is a good idea to
 * avoid changing this class manually, since it will be overwritten next time code is
 * regenerated. If you need to make any customizations, put them in a subclass.
 */
public abstract class _ClientMtTooneDep extends PersistentObject {

    public static final String TO_MASTER_PROPERTY = "toMaster";

    protected ValueHolder toMaster;

    public ClientMtTooneMaster getToMaster() {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "toMaster", true);
        }

        return (ClientMtTooneMaster) toMaster.getValue();
    }
    public void setToMaster(ClientMtTooneMaster toMaster) {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "toMaster", true);
        }

        this.toMaster.setValue(toMaster);
    }

}
