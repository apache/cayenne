package org.apache.cayenne.testdo.mt.auto;

import org.apache.cayenne.PersistentObject;

/**
 * A generated persistent class mapped as "MtTableBool" Cayenne entity. It is a good idea to
 * avoid changing this class manually, since it will be overwritten next time code is
 * regenerated. If you need to make any customizations, put them in a subclass.
 */
public abstract class _ClientMtTableBool extends PersistentObject {

    public static final String BLABLACHECK_PROPERTY = "blablacheck";
    public static final String NUMBER_PROPERTY = "number";

    protected boolean blablacheck;
    protected int number;

    public boolean isBlablacheck() {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "blablacheck", false);
        }

        return blablacheck;
    }
    public void setBlablacheck(boolean blablacheck) {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "blablacheck", false);
        }

        Object oldValue = this.blablacheck;
        this.blablacheck = blablacheck;

        // notify objectContext about simple property change
        if(objectContext != null) {
            objectContext.propertyChanged(this, "blablacheck", oldValue, blablacheck);
        }
    }

    public int getNumber() {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "number", false);
        }

        return number;
    }
    public void setNumber(int number) {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "number", false);
        }

        Object oldValue = this.number;
        this.number = number;

        // notify objectContext about simple property change
        if(objectContext != null) {
            objectContext.propertyChanged(this, "number", oldValue, number);
        }
    }

}
