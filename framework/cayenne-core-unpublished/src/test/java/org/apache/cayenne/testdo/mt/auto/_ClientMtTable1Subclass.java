package org.apache.cayenne.testdo.mt.auto;

import org.apache.cayenne.testdo.mt.ClientMtTable1;

/**
 * A generated persistent class mapped as "MtTable1Subclass" Cayenne entity. It is a good idea to
 * avoid changing this class manually, since it will be overwritten next time code is
 * regenerated. If you need to make any customizations, put them in a subclass.
 */
public abstract class _ClientMtTable1Subclass extends ClientMtTable1 {

    public static final String SUBCLASS_ATTRIBUTE1_PROPERTY = "subclassAttribute1";

    protected String subclassAttribute1;

    public String getSubclassAttribute1() {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "subclassAttribute1", false);
        }

        return subclassAttribute1;
    }
    public void setSubclassAttribute1(String subclassAttribute1) {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "subclassAttribute1", false);
        }

        Object oldValue = this.subclassAttribute1;
        this.subclassAttribute1 = subclassAttribute1;

        // notify objectContext about simple property change
        if(objectContext != null) {
            objectContext.propertyChanged(this, "subclassAttribute1", oldValue, subclassAttribute1);
        }
    }

}
