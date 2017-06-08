package org.apache.cayenne.tutorial.persistent.client.auto;

import org.apache.cayenne.PersistentObject;
import org.apache.cayenne.exp.Property;
import org.apache.cayenne.tutorial.persistent.client.Painting;
import org.apache.cayenne.util.PersistentObjectList;

import java.time.LocalDate;
import java.util.List;

/**
 * A generated persistent class mapped as "Artist" Cayenne entity. It is a good idea to
 * avoid changing this class manually, since it will be overwritten next time code is
 * regenerated. If you need to make any customizations, put them in a subclass.
 */
public abstract class _Artist extends PersistentObject {

    public static final Property<LocalDate> DATE_OF_BIRTH = Property.create("dateOfBirth", LocalDate.class);
    public static final Property<String> NAME = Property.create("name", String.class);
    public static final Property<List<Painting>> PAINTINGS = Property.create("paintings", List.class);

    protected LocalDate dateOfBirth;
    protected String name;
    protected List<Painting> paintings;

    public LocalDate getDateOfBirth() {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "dateOfBirth", false);
        }

        return dateOfBirth;
    }
    public void setDateOfBirth(LocalDate dateOfBirth) {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "dateOfBirth", false);
        }

        Object oldValue = this.dateOfBirth;
        // notify objectContext about simple property change
        if(objectContext != null) {
            objectContext.propertyChanged(this, "dateOfBirth", oldValue, dateOfBirth);
        }
        
        this.dateOfBirth = dateOfBirth;
    }

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
