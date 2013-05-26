package org.apache.cayenne.tutorial.persistent.client.auto;

import java.util.List;

import org.apache.cayenne.PersistentObject;
import org.apache.cayenne.exp.Property;
import org.apache.cayenne.tutorial.persistent.client.Painting;
import org.apache.cayenne.util.PersistentObjectList;

/**
 * A generated persistent class mapped as "Gallery" Cayenne entity. It is a good idea to
 * avoid changing this class manually, since it will be overwritten next time code is
 * regenerated. If you need to make any customizations, put them in a subclass.
 */
public abstract class _Gallery extends PersistentObject {

    @Deprecated
    public static final String NAME_PROPERTY = "name";
    @Deprecated
    public static final String PAINTINGS_PROPERTY = "paintings";

    public static final Property<String> NAME = new Property<String>("name");
    public static final Property<List<Painting>> PAINTINGS = new Property<List<Painting>>("paintings");

    protected String name;
    protected List<Painting> paintings;

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
        // notify objectContext about simple property change
        if(objectContext != null) {
            objectContext.propertyChanged(this, "name", oldValue, name);
        }
        
        this.name = name;
    }

    public List<Painting> getPaintings() {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "paintings", true);
        } else if (this.paintings == null) {
        	this.paintings = new PersistentObjectList(this, "paintings");
		}

        return paintings;
    }
    public void addToPaintings(Painting object) {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "paintings", true);
        } else if (this.paintings == null) {
        	this.paintings = new PersistentObjectList(this, "paintings");
		}

        this.paintings.add(object);
    }
    public void removeFromPaintings(Painting object) {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "paintings", true);
        } else if (this.paintings == null) {
        	this.paintings = new PersistentObjectList(this, "paintings");
		}

        this.paintings.remove(object);
    }

}
