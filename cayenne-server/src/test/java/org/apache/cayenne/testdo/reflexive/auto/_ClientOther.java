package org.apache.cayenne.testdo.reflexive.auto;

import java.util.List;

import org.apache.cayenne.PersistentObject;
import org.apache.cayenne.exp.property.ListProperty;
import org.apache.cayenne.exp.property.PropertyFactory;
import org.apache.cayenne.exp.property.StringProperty;
import org.apache.cayenne.testdo.reflexive.ClientReflexive;
import org.apache.cayenne.util.PersistentObjectList;

/**
 * A generated persistent class mapped as "Other" Cayenne entity. It is a good idea to
 * avoid changing this class manually, since it will be overwritten next time code is
 * regenerated. If you need to make any customizations, put them in a subclass.
 */
public abstract class _ClientOther extends PersistentObject {

    public static final StringProperty<String> NAME = PropertyFactory.createString("name", String.class);
    public static final ListProperty<ClientReflexive> REFLEXIVES = PropertyFactory.createList("reflexives", ClientReflexive.class);

    protected String name;
    protected List<ClientReflexive> reflexives;

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

    public List<ClientReflexive> getReflexives() {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "reflexives", true);
        } else if (this.reflexives == null) {
        	this.reflexives = new PersistentObjectList<>(this, "reflexives");
		}

        return reflexives;
    }

    public void addToReflexives(ClientReflexive object) {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "reflexives", true);
        } else if (this.reflexives == null) {
        	this.reflexives = new PersistentObjectList<>(this, "reflexives");
		}

        this.reflexives.add(object);
    }

    public void removeFromReflexives(ClientReflexive object) {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "reflexives", true);
        } else if (this.reflexives == null) {
        	this.reflexives = new PersistentObjectList<>(this, "reflexives");
		}

        this.reflexives.remove(object);
    }

}
