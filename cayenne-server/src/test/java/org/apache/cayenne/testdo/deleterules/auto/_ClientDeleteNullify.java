package org.apache.cayenne.testdo.deleterules.auto;

import org.apache.cayenne.PersistentObject;
import org.apache.cayenne.ValueHolder;
import org.apache.cayenne.testdo.deleterules.ClientDeleteRule;

/**
 * A generated persistent class mapped as "DeleteNullify" Cayenne entity. It is a good idea to
 * avoid changing this class manually, since it will be overwritten next time code is
 * regenerated. If you need to make any customizations, put them in a subclass.
 */
public abstract class _ClientDeleteNullify extends PersistentObject {

    public static final String NAME_PROPERTY = "name";
    public static final String NULLIFY_PROPERTY = "nullify";

    protected String name;
    protected ValueHolder nullify;

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

    public ClientDeleteRule getNullify() {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "nullify", true);
        }

        return (ClientDeleteRule) nullify.getValue();
    }
    public void setNullify(ClientDeleteRule nullify) {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "nullify", true);
        }

        this.nullify.setValue(nullify);
    }

}
