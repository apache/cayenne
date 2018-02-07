package org.apache.cayenne.rop.protostuff.persistent.auto;

import org.apache.cayenne.PersistentObject;
import org.apache.cayenne.ValueHolder;
import org.apache.cayenne.exp.Property;
import org.apache.cayenne.rop.protostuff.persistent.ClientMtTable1;
import org.apache.cayenne.util.PersistentObjectHolder;

/**
 * A generated persistent class mapped as "MtTable2" Cayenne entity. It is a good idea to
 * avoid changing this class manually, since it will be overwritten next time code is
 * regenerated. If you need to make any customizations, put them in a subclass.
 */
public abstract class _ClientMtTable2 extends PersistentObject {

    public static final Property<String> GLOBAL_ATTRIBUTE = Property.create("globalAttribute", String.class);
    public static final Property<ClientMtTable1> TABLE1 = Property.create("table1", ClientMtTable1.class);

    protected String globalAttribute;
    protected ValueHolder<ClientMtTable1> table1;

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
            objectContext.propertyChanged(this, "table1", this.table1.getValueDirectly(), table1);
        } else if (this.table1 == null) {
        	this.table1 = new PersistentObjectHolder<>(this, "table1");
		}

        this.table1.setValue(table1);
    }

}
