package org.apache.cayenne.tutorial.persistent.client.auto;

import java.time.LocalDate;
import java.util.List;

import org.apache.cayenne.PersistentObject;
import org.apache.cayenne.exp.property.DateProperty;
import org.apache.cayenne.exp.property.ListProperty;
import org.apache.cayenne.exp.property.PropertyFactory;
import org.apache.cayenne.exp.property.StringProperty;
import org.apache.cayenne.tutorial.persistent.client.Painting;
import org.apache.cayenne.util.PersistentObjectList;

/**
 * A generated persistent class mapped as "Artist" Cayenne entity. It is a good idea to
 * avoid changing this class manually, since it will be overwritten next time code is
 * regenerated. If you need to make any customizations, put them in a subclass.
 */
public abstract class _Artist extends PersistentObject {

    public static final DateProperty<LocalDate> DATE_OF_BIRTH = PropertyFactory.createDate("dateOfBirth", LocalDate.class);
    public static final StringProperty<String> NAME = PropertyFactory.createString("name", String.class);
    public static final ListProperty<Painting> PAINTINGS = PropertyFactory.createList("paintings", Painting.class);

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
            objectContext.propertyChanged(this, "dateOfBirth", this.dateOfBirth, dateOfBirth);
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
            objectContext.propertyChanged(this, "name", this.name, name);
        }

        this.name = name;
    }

    public List<Painting> getPaintings() {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "paintings", true);
        } else if (this.paintings == null) {
        	this.paintings = new PersistentObjectList<>(this, "paintings");
		}

        return paintings;
    }

    public void addToPaintings(Painting object) {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "paintings", true);
        } else if (this.paintings == null) {
        	this.paintings = new PersistentObjectList<>(this, "paintings");
		}

        this.paintings.add(object);
    }

    public void removeFromPaintings(Painting object) {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "paintings", true);
        } else if (this.paintings == null) {
        	this.paintings = new PersistentObjectList<>(this, "paintings");
		}

        this.paintings.remove(object);
    }

}
