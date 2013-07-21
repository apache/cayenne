package org.apache.cayenne.testdo.mt.auto;

import org.apache.cayenne.PersistentObject;

/**
 * A generated persistent class mapped as "MtMeaningfulPk" Cayenne entity. It is a good idea to
 * avoid changing this class manually, since it will be overwritten next time code is
 * regenerated. If you need to make any customizations, put them in a subclass.
 */
public abstract class _ClientMtMeaningfulPk extends PersistentObject {

    public static final String PK_PROPERTY = "pk";

    protected String pk;

    public String getPk() {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "pk", false);
        }

        return pk;
    }
    public void setPk(String pk) {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "pk", false);
        }

        Object oldValue = this.pk;
        this.pk = pk;

        // notify objectContext about simple property change
        if(objectContext != null) {
            objectContext.propertyChanged(this, "pk", oldValue, pk);
        }
    }

}
