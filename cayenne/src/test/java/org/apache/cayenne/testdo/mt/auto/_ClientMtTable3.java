package org.apache.cayenne.testdo.mt.auto;

import java.util.List;

import org.apache.cayenne.PersistentObject;
import org.apache.cayenne.exp.property.BaseProperty;
import org.apache.cayenne.exp.property.ListProperty;
import org.apache.cayenne.exp.property.NumericProperty;
import org.apache.cayenne.exp.property.PropertyFactory;
import org.apache.cayenne.exp.property.StringProperty;
import org.apache.cayenne.testdo.mt.ClientMtTable2;
import org.apache.cayenne.util.PersistentObjectList;

/**
 * A generated persistent class mapped as "MtTable3" Cayenne entity. It is a good idea to
 * avoid changing this class manually, since it will be overwritten next time code is
 * regenerated. If you need to make any customizations, put them in a subclass.
 */
public abstract class _ClientMtTable3 extends PersistentObject {

    public static final BaseProperty<byte[]> BINARY_COLUMN = PropertyFactory.createBase("binaryColumn", byte[].class);
    public static final StringProperty<String> CHAR_COLUMN = PropertyFactory.createString("charColumn", String.class);
    public static final NumericProperty<Integer> INT_COLUMN = PropertyFactory.createNumeric("intColumn", Integer.class);
    public static final ListProperty<ClientMtTable2> TABLE2ARRAY = PropertyFactory.createList("table2Array", ClientMtTable2.class);

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
            objectContext.propertyChanged(this, "binaryColumn", this.binaryColumn, binaryColumn);
        }

        this.binaryColumn = binaryColumn;
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
            objectContext.propertyChanged(this, "charColumn", this.charColumn, charColumn);
        }

        this.charColumn = charColumn;
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
            objectContext.propertyChanged(this, "intColumn", this.intColumn, intColumn);
        }

        this.intColumn = intColumn;
    }

    public List<ClientMtTable2> getTable2Array() {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "table2Array", true);
        } else if (this.table2Array == null) {
        	this.table2Array = new PersistentObjectList<>(this, "table2Array");
		}

        return table2Array;
    }

    public void addToTable2Array(ClientMtTable2 object) {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "table2Array", true);
        } else if (this.table2Array == null) {
        	this.table2Array = new PersistentObjectList<>(this, "table2Array");
		}

        this.table2Array.add(object);
    }

    public void removeFromTable2Array(ClientMtTable2 object) {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "table2Array", true);
        } else if (this.table2Array == null) {
        	this.table2Array = new PersistentObjectList<>(this, "table2Array");
		}

        this.table2Array.remove(object);
    }

}
