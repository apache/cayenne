package org.apache.cayenne.testdo.mt.auto;

import java.util.List;

import org.apache.cayenne.PersistentObject;
import org.apache.cayenne.ValueHolder;
import org.apache.cayenne.testdo.mt.ClientMtReflexive;

/**
 * A generated persistent class mapped as "MtReflexive" Cayenne entity. It is a good idea to
 * avoid changing this class manually, since it will be overwritten next time code is
 * regenerated. If you need to make any customizations, put them in a subclass.
 */
public abstract class _ClientMtReflexive extends PersistentObject {

    public static final String NAME_PROPERTY = "name";
    public static final String CHILDREN_PROPERTY = "children";
    public static final String TO_PARENT_PROPERTY = "toParent";

    protected String name;
    protected List<ClientMtReflexive> children;
    protected ValueHolder toParent;

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

    public List<ClientMtReflexive> getChildren() {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "children", true);
        }

        return children;
    }
    public void addToChildren(ClientMtReflexive object) {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "children", true);
        }

        this.children.add(object);
    }
    public void removeFromChildren(ClientMtReflexive object) {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "children", true);
        }

        this.children.remove(object);
    }

    public ClientMtReflexive getToParent() {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "toParent", true);
        }

        return (ClientMtReflexive) toParent.getValue();
    }
    public void setToParent(ClientMtReflexive toParent) {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "toParent", true);
        }

        this.toParent.setValue(toParent);
    }

}
