package org.apache.cayenne.itest.cpa.defaults.client.auto;

import org.apache.cayenne.PersistentObject;
import org.apache.cayenne.ValueHolder;
import org.apache.cayenne.itest.cpa.defaults.client.DefaultsTable1;

/**
 * A generated persistent class mapped as "DefaultsTable2" Cayenne entity. It is a good idea to
 * avoid changing this class manually, since it will be overwritten next time code is
 * regenerated. If you need to make any customizations, put them in a subclass.
 */
public class _DefaultsTable2 extends PersistentObject {

    public static final String TO_TABLE1_PROPERTY = "toTable1";

    protected ValueHolder toTable1;

    public DefaultsTable1 getToTable1() {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "toTable1", true);
        }
        
        return (DefaultsTable1) toTable1.getValue();
    }
    public void setToTable1(DefaultsTable1 toTable1) {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "toTable1", true);
        }
        
        this.toTable1.setValue(toTable1);
    }
    
}
