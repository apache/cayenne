package org.apache.cayenne.testdo.mt.auto;

import org.apache.cayenne.PersistentObject;
import org.apache.cayenne.ValueHolder;
import org.apache.cayenne.testdo.mt.ClientMtDeleteRule;

/**
 * A generated persistent class mapped as "MtDeleteCascade" Cayenne entity. It is a good idea to
 * avoid changing this class manually, since it will be overwritten next time code is
 * regenerated. If you need to make any customizations, put them in a subclass.
 */
public class _ClientMtDeleteCascade extends PersistentObject {

    public static final String NAME_PROPERTY = "name";
    public static final String CASCADE_PROPERTY = "cascade";

    protected String name;
    protected ValueHolder cascade;

    public String getName() {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "name", false);
        }
        
        return name;
    }
    public void setName(String name) {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "name", false);
        }
        
        Object oldValue = this.name;
        this.name = name;
        
        // notify objectContext about simple property change
        if(objectContext != null) {
            objectContext.propertyChanged(this, "name", oldValue, name);
        }
    }
    
    
    public ClientMtDeleteRule getCascade() {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "cascade", true);
        }
        
        return (ClientMtDeleteRule) cascade.getValue();
    }
    public void setCascade(ClientMtDeleteRule cascade) {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "cascade", true);
        }
        
        this.cascade.setValue(cascade);
    }
    
}
