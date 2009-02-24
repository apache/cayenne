package org.apache.cayenne.testdo.mt.auto;

import org.apache.cayenne.PersistentObject;
import org.apache.cayenne.ValueHolder;
import org.apache.cayenne.testdo.mt.ClientMtTable1;
import org.apache.cayenne.testdo.mt.ClientMtTable3;

/**
 * A generated persistent class mapped as "MtTable2" Cayenne entity. It is a good idea to
 * avoid changing this class manually, since it will be overwritten next time code is
 * regenerated. If you need to make any customizations, put them in a subclass.
 */
public abstract class _ClientMtTable2 extends PersistentObject {

    public static final String GLOBAL_ATTRIBUTE_PROPERTY = "globalAttribute";
    public static final String TABLE1_PROPERTY = "table1";
    public static final String TABLE3_PROPERTY = "table3";

    protected String globalAttribute;
    protected ValueHolder table1;
    protected ValueHolder table3;

    public String getGlobalAttribute() {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "globalAttribute", false);
        }

        return globalAttribute;
    }
    public void setGlobalAttribute(String globalAttribute) {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "globalAttribute", false);
        }

        Object oldValue = this.globalAttribute;
        this.globalAttribute = globalAttribute;

        // notify objectContext about simple property change
        if(objectContext != null) {
            objectContext.propertyChanged(this, "globalAttribute", oldValue, globalAttribute);
        }
    }

    public ClientMtTable1 getTable1() {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "table1", true);
        }

        return (ClientMtTable1) table1.getValue();
    }
    public void setTable1(ClientMtTable1 table1) {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "table1", true);
        }

        this.table1.setValue(table1);
    }

    public ClientMtTable3 getTable3() {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "table3", true);
        }

        return (ClientMtTable3) table3.getValue();
    }
    public void setTable3(ClientMtTable3 table3) {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "table3", true);
        }

        this.table3.setValue(table3);
    }

}
