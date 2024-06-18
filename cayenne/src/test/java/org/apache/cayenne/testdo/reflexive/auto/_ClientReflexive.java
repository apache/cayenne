package org.apache.cayenne.testdo.reflexive.auto;

import org.apache.cayenne.PersistentObject;
import org.apache.cayenne.ValueHolder;
import org.apache.cayenne.testdo.reflexive.ClientReflexive;

import java.util.List;

/**
 * A generated persistent class mapped as "Reflexive" Cayenne entity. It is a good idea to
 * avoid changing this class manually, since it will be overwritten next time code is
 * regenerated. If you need to make any customizations, put them in a subclass.
 */
public abstract class _ClientReflexive extends PersistentObject {

    public static final String NAME_PROPERTY = "name";
    public static final String CHILDREN_PROPERTY = "children";
    public static final String TO_PARENT_PROPERTY = "toParent";

    protected String name;
    protected List<ClientReflexive> children;
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

    public List<ClientReflexive> getChildren() {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "children", true);
        }

        return children;
    }
    public void addToChildren(ClientReflexive object) {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "children", true);
        }

        this.children.add(object);
    }
    public void removeFromChildren(ClientReflexive object) {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "children", true);
        }

        this.children.remove(object);
    }

    public ClientReflexive getToParent() {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "toParent", true);
        }

        return (ClientReflexive) toParent.getValue();
    }
    public void setToParent(ClientReflexive toParent) {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "toParent", true);
        }

        this.toParent.setValue(toParent);
    }

}
