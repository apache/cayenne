package org.apache.cayenne.testdo.toone.auto;

import org.apache.cayenne.PersistentObject;
import org.apache.cayenne.ValueHolder;
import org.apache.cayenne.testdo.toone.ClientTooneDep;

/**
 * A generated persistent class mapped as "TooneMaster" Cayenne entity. It is a good idea to
 * avoid changing this class manually, since it will be overwritten next time code is
 * regenerated. If you need to make any customizations, put them in a subclass.
 */
public abstract class _ClientTooneMaster extends PersistentObject {

    public static final String TO_DEPENDENT_PROPERTY = "toDependent";

    protected ValueHolder toDependent;

    public ClientTooneDep getToDependent() {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "toDependent", true);
        }

        return (ClientTooneDep) toDependent.getValue();
    }
    public void setToDependent(ClientTooneDep toDependent) {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "toDependent", true);
        }

        this.toDependent.setValue(toDependent);
    }

}
