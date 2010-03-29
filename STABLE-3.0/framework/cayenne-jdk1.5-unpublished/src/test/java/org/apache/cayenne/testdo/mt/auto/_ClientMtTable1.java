package org.apache.cayenne.testdo.mt.auto;

import java.util.List;

import org.apache.cayenne.PersistentObject;
import org.apache.cayenne.testdo.mt.ClientMtTable2;

/**
 * A generated persistent class mapped as "MtTable1" Cayenne entity. It is a good idea to
 * avoid changing this class manually, since it will be overwritten next time code is
 * regenerated. If you need to make any customizations, put them in a subclass.
 */
public abstract class _ClientMtTable1 extends PersistentObject {

    public static final String GLOBAL_ATTRIBUTE1_PROPERTY = "globalAttribute1";
    public static final String SERVER_ATTRIBUTE1_PROPERTY = "serverAttribute1";
    public static final String TABLE2ARRAY_PROPERTY = "table2Array";

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
        }

        Object oldValue = this.globalAttribute1;
        this.globalAttribute1 = globalAttribute1;

        // notify objectContext about simple property change
        if(objectContext != null) {
            objectContext.propertyChanged(this, "globalAttribute1", oldValue, globalAttribute1);
        }
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
        }

        Object oldValue = this.serverAttribute1;
        this.serverAttribute1 = serverAttribute1;

        // notify objectContext about simple property change
        if(objectContext != null) {
            objectContext.propertyChanged(this, "serverAttribute1", oldValue, serverAttribute1);
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
