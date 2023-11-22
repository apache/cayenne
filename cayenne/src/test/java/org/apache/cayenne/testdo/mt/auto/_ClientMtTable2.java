package org.apache.cayenne.testdo.mt.auto;

import org.apache.cayenne.PersistentObject;
import org.apache.cayenne.ValueHolder;
import org.apache.cayenne.exp.property.EntityProperty;
import org.apache.cayenne.exp.property.PropertyFactory;
import org.apache.cayenne.exp.property.StringProperty;
import org.apache.cayenne.testdo.mt.ClientMtTable1;
import org.apache.cayenne.testdo.mt.ClientMtTable3;
import org.apache.cayenne.util.PersistentObjectHolder;

/**
 * A generated persistent class mapped as "MtTable2" Cayenne entity. It is a good idea to
 * avoid changing this class manually, since it will be overwritten next time code is
 * regenerated. If you need to make any customizations, put them in a subclass.
 */
public abstract class _ClientMtTable2 extends PersistentObject {

    public static final StringProperty<String> GLOBAL_ATTRIBUTE = PropertyFactory.createString("globalAttribute", String.class);
    public static final EntityProperty<ClientMtTable1> TABLE1 = PropertyFactory.createEntity("table1", ClientMtTable1.class);
    public static final EntityProperty<ClientMtTable3> TABLE3 = PropertyFactory.createEntity("table3", ClientMtTable3.class);

    protected String globalAttribute;
    protected ValueHolder<ClientMtTable1> table1;
    protected ValueHolder<ClientMtTable3> table3;

    public String getGlobalAttribute() {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "globalAttribute", false);
        }

        return globalAttribute;
    }

    public void setGlobalAttribute(String globalAttribute) {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "globalAttribute", false);
            objectContext.propertyChanged(this, "globalAttribute", this.globalAttribute, globalAttribute);
        }

        this.globalAttribute = globalAttribute;
    }

    public ClientMtTable1 getTable1() {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "table1", true);
        } else if (this.table1 == null) {
        	this.table1 = new PersistentObjectHolder<>(this, "table1");
		}

        return table1.getValue();
    }

    public void setTable1(ClientMtTable1 table1) {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "table1", true);
        } else if (this.table1 == null) {
        	this.table1 = new PersistentObjectHolder<>(this, "table1");
		}

        this.table1.setValue(table1);
    }

    public ClientMtTable3 getTable3() {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "table3", true);
        } else if (this.table3 == null) {
        	this.table3 = new PersistentObjectHolder<>(this, "table3");
		}

        return table3.getValue();
    }

    public void setTable3(ClientMtTable3 table3) {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "table3", true);
        } else if (this.table3 == null) {
        	this.table3 = new PersistentObjectHolder<>(this, "table3");
		}

        this.table3.setValue(table3);
    }

}
