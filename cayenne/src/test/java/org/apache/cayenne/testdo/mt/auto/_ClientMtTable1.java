package org.apache.cayenne.testdo.mt.auto;

import java.util.List;

import org.apache.cayenne.PersistentObject;
import org.apache.cayenne.exp.property.ListProperty;
import org.apache.cayenne.exp.property.PropertyFactory;
import org.apache.cayenne.exp.property.StringProperty;
import org.apache.cayenne.testdo.mt.ClientMtTable2;
import org.apache.cayenne.util.PersistentObjectList;

/**
 * A generated persistent class mapped as "MtTable1" Cayenne entity. It is a good idea to
 * avoid changing this class manually, since it will be overwritten next time code is
 * regenerated. If you need to make any customizations, put them in a subclass.
 */
public abstract class _ClientMtTable1 extends PersistentObject {

    public static final StringProperty<String> GLOBAL_ATTRIBUTE1 = PropertyFactory.createString("globalAttribute1", String.class);
    public static final StringProperty<String> SERVER_ATTRIBUTE1 = PropertyFactory.createString("serverAttribute1", String.class);
    public static final ListProperty<ClientMtTable2> TABLE2ARRAY = PropertyFactory.createList("table2Array", ClientMtTable2.class);

    protected String globalAttribute1;
    protected String serverAttribute1;
    protected List<ClientMtTable2> table2Array;

    public String getGlobalAttribute1() {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "globalAttribute1", false);
        }

        return globalAttribute1;
    }

    public void setGlobalAttribute1(String globalAttribute1) {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "globalAttribute1", false);
            objectContext.propertyChanged(this, "globalAttribute1", this.globalAttribute1, globalAttribute1);
        }

        this.globalAttribute1 = globalAttribute1;
    }

    public String getServerAttribute1() {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "serverAttribute1", false);
        }

        return serverAttribute1;
    }

    public void setServerAttribute1(String serverAttribute1) {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "serverAttribute1", false);
            objectContext.propertyChanged(this, "serverAttribute1", this.serverAttribute1, serverAttribute1);
        }

        this.serverAttribute1 = serverAttribute1;
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
