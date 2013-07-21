package org.apache.cayenne.testdo.mt.auto;

import org.apache.cayenne.PersistentObject;
import org.apache.cayenne.ValueHolder;
import org.apache.cayenne.testdo.mt.ClientMtTooneDep;

/**
 * A generated persistent class mapped as "MtTooneMaster" Cayenne entity. It is a good idea to
 * avoid changing this class manually, since it will be overwritten next time code is
 * regenerated. If you need to make any customizations, put them in a subclass.
 */
public abstract class _ClientMtTooneMaster extends PersistentObject {

    public static final String TO_DEPENDENT_PROPERTY = "toDependent";

    protected ValueHolder toDependent;

    public ClientMtTooneDep getToDependent() {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "toDependent", true);
        }

        return (ClientMtTooneDep) toDependent.getValue();
    }
    public void setToDependent(ClientMtTooneDep toDependent) {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "toDependent", true);
        }

        this.toDependent.setValue(toDependent);
    }

}
