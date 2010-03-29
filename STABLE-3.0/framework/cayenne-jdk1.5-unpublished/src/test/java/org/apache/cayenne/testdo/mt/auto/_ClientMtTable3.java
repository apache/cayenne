package org.apache.cayenne.testdo.mt.auto;

import java.util.List;

import org.apache.cayenne.PersistentObject;
import org.apache.cayenne.testdo.mt.ClientMtTable2;

/**
 * A generated persistent class mapped as "MtTable3" Cayenne entity. It is a good idea to
 * avoid changing this class manually, since it will be overwritten next time code is
 * regenerated. If you need to make any customizations, put them in a subclass.
 */
public abstract class _ClientMtTable3 extends PersistentObject {

    public static final String BINARY_COLUMN_PROPERTY = "binaryColumn";
    public static final String CHAR_COLUMN_PROPERTY = "charColumn";
    public static final String INT_COLUMN_PROPERTY = "intColumn";
    public static final String TABLE2ARRAY_PROPERTY = "table2Array";

    protected byte[] binaryColumn;
    protected String charColumn;
    protected Integer intColumn;
    protected List<ClientMtTable2> table2Array;

    public byte[] getBinaryColumn() {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "binaryColumn", false);
        }

        return binaryColumn;
    }
    public void setBinaryColumn(byte[] binaryColumn) {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "binaryColumn", false);
        }

        Object oldValue = this.binaryColumn;
        this.binaryColumn = binaryColumn;

        // notify objectContext about simple property change
        if(objectContext != null) {
            objectContext.propertyChanged(this, "binaryColumn", oldValue, binaryColumn);
        }
    }

    public String getCharColumn() {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "charColumn", false);
        }

        return charColumn;
    }
    public void setCharColumn(String charColumn) {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "charColumn", false);
        }

        Object oldValue = this.charColumn;
        this.charColumn = charColumn;

        // notify objectContext about simple property change
        if(objectContext != null) {
            objectContext.propertyChanged(this, "charColumn", oldValue, charColumn);
        }
    }

    public Integer getIntColumn() {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "intColumn", false);
        }

        return intColumn;
    }
    public void setIntColumn(Integer intColumn) {
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

    public List<ClientMtTable2> getTable2Array() {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "table2Array", true);
        }

        return table2Array;
    }
    public void addToTable2Array(ClientMtTable2 object) {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "table2Array", true);
        }

        this.table2Array.add(object);
    }
    public void removeFromTable2Array(ClientMtTable2 object) {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "table2Array", true);
        }

        this.table2Array.remove(object);
    }

}
