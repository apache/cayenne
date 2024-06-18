package org.apache.cayenne.testdo.deleterules.auto;

import org.apache.cayenne.PersistentObject;
import org.apache.cayenne.ValueHolder;
import org.apache.cayenne.testdo.deleterules.ClientDeleteRule;

/**
 * A generated persistent class mapped as "DeleteDeny" Cayenne entity. It is a good idea to
 * avoid changing this class manually, since it will be overwritten next time code is
 * regenerated. If you need to make any customizations, put them in a subclass.
 */
public abstract class _ClientDeleteDeny extends PersistentObject {

    public static final String NAME_PROPERTY = "name";
    public static final String DENY_PROPERTY = "deny";

    protected String name;
    protected ValueHolder deny;

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

    public ClientDeleteRule getDeny() {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "deny", true);
        }

        return (ClientDeleteRule) deny.getValue();
    }
    public void setDeny(ClientDeleteRule deny) {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "deny", true);
        }

        this.deny.setValue(deny);
    }

}
