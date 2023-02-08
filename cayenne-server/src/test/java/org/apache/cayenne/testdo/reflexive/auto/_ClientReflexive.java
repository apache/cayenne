package org.apache.cayenne.testdo.reflexive.auto;

import java.util.List;

import org.apache.cayenne.PersistentObject;
import org.apache.cayenne.ValueHolder;
import org.apache.cayenne.exp.property.EntityProperty;
import org.apache.cayenne.exp.property.ListProperty;
import org.apache.cayenne.exp.property.PropertyFactory;
import org.apache.cayenne.exp.property.StringProperty;
import org.apache.cayenne.testdo.reflexive.ClientOther;
import org.apache.cayenne.testdo.reflexive.ClientReflexive;
import org.apache.cayenne.util.PersistentObjectHolder;
import org.apache.cayenne.util.PersistentObjectList;

/**
 * A generated persistent class mapped as "Reflexive" Cayenne entity. It is a good idea to
 * avoid changing this class manually, since it will be overwritten next time code is
 * regenerated. If you need to make any customizations, put them in a subclass.
 */
public abstract class _ClientReflexive extends PersistentObject {

    public static final StringProperty<String> NAME = PropertyFactory.createString("name", String.class);
    public static final ListProperty<ClientReflexive> CHILDREN = PropertyFactory.createList("children", ClientReflexive.class);
    public static final EntityProperty<ClientOther> TO_OTHER = PropertyFactory.createEntity("toOther", ClientOther.class);
    public static final EntityProperty<ClientReflexive> TO_PARENT = PropertyFactory.createEntity("toParent", ClientReflexive.class);

    protected String name;
    protected List<ClientReflexive> children;
    protected ValueHolder<ClientOther> toOther;
    protected ValueHolder<ClientReflexive> toParent;

    public String getName() {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "name", false);
        }


        return name;
    }

    public void setName(String name) {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "name", false);
            objectContext.propertyChanged(this, "name", this.name, name);
        }

        this.name = name;
    }

    public List<ClientReflexive> getChildren() {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "children", true);
        } else if (this.children == null) {
        	this.children = new PersistentObjectList<>(this, "children");
		}

        return children;
    }

    public void addToChildren(ClientReflexive object) {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "children", true);
        } else if (this.children == null) {
        	this.children = new PersistentObjectList<>(this, "children");
		}

        this.children.add(object);
    }

    public void removeFromChildren(ClientReflexive object) {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "children", true);
        } else if (this.children == null) {
        	this.children = new PersistentObjectList<>(this, "children");
		}

        this.children.remove(object);
    }

    public ClientOther getToOther() {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "toOther", true);
        } else if (this.toOther == null) {
        	this.toOther = new PersistentObjectHolder<>(this, "toOther");
		}

        return toOther.getValue();
    }

    public void setToOther(ClientOther toOther) {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "toOther", true);
        } else if (this.toOther == null) {
        	this.toOther = new PersistentObjectHolder<>(this, "toOther");
		}

        this.toOther.setValue(toOther);
    }

    public ClientReflexive getToParent() {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "toParent", true);
        } else if (this.toParent == null) {
        	this.toParent = new PersistentObjectHolder<>(this, "toParent");
		}

        return toParent.getValue();
    }

    public void setToParent(ClientReflexive toParent) {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "toParent", true);
        } else if (this.toParent == null) {
        	this.toParent = new PersistentObjectHolder<>(this, "toParent");
		}

        this.toParent.setValue(toParent);
    }

}
