package org.apache.cayenne.testdo.mt.auto;

import org.apache.cayenne.PersistentObject;

/**
 * A generated persistent class mapped as "MtTablePrimitives" Cayenne entity. It is a good idea to
 * avoid changing this class manually, since it will be overwritten next time code is
 * regenerated. If you need to make any customizations, put them in a subclass.
 */
public abstract class _ClientMtTablePrimitives extends PersistentObject {

    public static final String BOOLEAN_COLUMN_PROPERTY = "booleanColumn";
    public static final String INT_COLUMN_PROPERTY = "intColumn";

    protected boolean booleanColumn;
    protected int intColumn;

    public boolean isBooleanColumn() {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "booleanColumn", false);
        }

        return booleanColumn;
    }
    public void setBooleanColumn(boolean booleanColumn) {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "booleanColumn", false);
        }

        Object oldValue = this.booleanColumn;
        this.booleanColumn = booleanColumn;

        // notify objectContext about simple property change
        if(objectContext != null) {
            objectContext.propertyChanged(this, "booleanColumn", oldValue, booleanColumn);
        }
    }

    public int getIntColumn() {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "intColumn", false);
        }

        return intColumn;
    }
    public void setIntColumn(int intColumn) {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "intColumn", false);
        }

        Object oldValue = this.intColumn;
        this.intColumn = intColumn;

        // notify objectContext about simple property change
        if(objectContext != null) {
            objectContext.propertyChanged(this, "intColumn", oldValue, intColumn);
        }
    }

}
