package org.apache.cayenne.testdo.cay_2641.client.auto;

import java.util.List;

import org.apache.cayenne.Fault;
import org.apache.cayenne.PersistentObject;
import org.apache.cayenne.exp.property.ListProperty;
import org.apache.cayenne.exp.property.PropertyFactory;
import org.apache.cayenne.exp.property.StringProperty;
import org.apache.cayenne.testdo.cay_2641.client.PaintingLazy;
import org.apache.cayenne.util.PersistentObjectList;

/**
 * A generated persistent class mapped as "ArtistLazy" Cayenne entity. It is a good idea to
 * avoid changing this class manually, since it will be overwritten next time code is
 * regenerated. If you need to make any customizations, put them in a subclass.
 */
public abstract class _ArtistLazy extends PersistentObject {

    public static final StringProperty<String> NAME = PropertyFactory.createString("name", String.class);
    public static final StringProperty<String> SURNAME = PropertyFactory.createString("surname", String.class);
    public static final ListProperty<PaintingLazy> PAINTINGS = PropertyFactory.createList("paintings", PaintingLazy.class);

    protected Object name;
    protected String surname;
    protected List<PaintingLazy> paintings;

    public String getName() {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "name", false);
        }

        if(this.name instanceof Fault) {
            this.name = ((Fault) this.name).resolveFault(this, "name");
        }

        return (String) name;
    }

    public void setName(String name) {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "name", false);
            objectContext.propertyChanged(this, "name", this.name, name);
        }

        this.name = name;
    }

    public String getSurname() {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "surname", false);
        }


        return surname;
    }

    public void setSurname(String surname) {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "surname", false);
            objectContext.propertyChanged(this, "surname", this.surname, surname);
        }

        this.surname = surname;
    }

    public List<PaintingLazy> getPaintings() {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "paintings", true);
        } else if (this.paintings == null) {
        	this.paintings = new PersistentObjectList<>(this, "paintings");
		}

        return paintings;
    }

    public void addToPaintings(PaintingLazy object) {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "paintings", true);
        } else if (this.paintings == null) {
        	this.paintings = new PersistentObjectList<>(this, "paintings");
		}

        this.paintings.add(object);
    }

    public void removeFromPaintings(PaintingLazy object) {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "paintings", true);
        } else if (this.paintings == null) {
        	this.paintings = new PersistentObjectList<>(this, "paintings");
		}

        this.paintings.remove(object);
    }

    public Object readPropertyDirectly(String propName) {
        if(propName == null) {
            throw new IllegalArgumentException();
        }

        switch(propName) {
            case "name":
                return this.name;
            case "surname":
                return this.surname;
            case "paintings":
                return this.paintings;
            default:
                return null;
        }
    }
}
