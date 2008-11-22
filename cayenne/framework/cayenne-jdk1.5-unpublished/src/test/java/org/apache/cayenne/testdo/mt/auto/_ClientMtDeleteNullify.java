package org.apache.cayenne.testdo.mt.auto;

import org.apache.cayenne.PersistentObject;
import org.apache.cayenne.ValueHolder;
import org.apache.cayenne.testdo.mt.ClientMtDeleteRule;

/**
 * A generated persistent class mapped as "MtDeleteNullify" Cayenne entity. It is a good idea to
 * avoid changing this class manually, since it will be overwritten next time code is
 * regenerated. If you need to make any customizations, put them in a subclass.
 */
public abstract class _ClientMtDeleteNullify extends PersistentObject {

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

    public ClientMtDeleteRule getNullify() {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "nullify", true);
        }

        return (ClientMtDeleteRule) nullify.getValue();
    }
    public void setNullify(ClientMtDeleteRule nullify) {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "nullify", true);
        }

        this.nullify.setValue(nullify);
    }

}
