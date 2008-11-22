package org.apache.cayenne.itest.cpa.defaults.client.auto;

import java.util.List;

import org.apache.cayenne.PersistentObject;
import org.apache.cayenne.itest.cpa.defaults.client.DefaultsTable4;

/**
 * A generated persistent class mapped as "DefaultsTable3" Cayenne entity. It is a good idea to
 * avoid changing this class manually, since it will be overwritten next time code is
 * regenerated. If you need to make any customizations, put them in a subclass.
 */
public class _DefaultsTable3 extends PersistentObject {

    public static final String NAME_PROPERTY = "name";
    public static final String DEFAULT_TABLE4S_PROPERTY = "defaultTable4s";

    protected String name;
    protected List defaultTable4s;

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
    
    
    public List getDefaultTable4s() {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "defaultTable4s", true);
        }
        
        return defaultTable4s;
    }
    public void addToDefaultTable4s(DefaultsTable4 object) {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "defaultTable4s", true);
        }
        
        this.defaultTable4s.add(object);
    }
    public void removeFromDefaultTable4s(DefaultsTable4 object) {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "defaultTable4s", true);
        }
        
        this.defaultTable4s.remove(object);
    }
    
}
