package org.apache.cayenne.tutorial.persistent.client.auto;

import org.apache.cayenne.PersistentObject;
import org.apache.cayenne.ValueHolder;
import org.apache.cayenne.exp.Property;
import org.apache.cayenne.tutorial.persistent.client.Artist;
import org.apache.cayenne.tutorial.persistent.client.Gallery;
import org.apache.cayenne.util.PersistentObjectHolder;

/**
 * A generated persistent class mapped as "Painting" Cayenne entity. It is a good idea to
 * avoid changing this class manually, since it will be overwritten next time code is
 * regenerated. If you need to make any customizations, put them in a subclass.
 */
public abstract class _Painting extends PersistentObject {

    @Deprecated
    public static final String NAME_PROPERTY = "name";
    @Deprecated
    public static final String ARTIST_PROPERTY = "artist";
    @Deprecated
    public static final String GALLERY_PROPERTY = "gallery";

    public static final Property<String> NAME = new Property<String>("name");
    public static final Property<Artist> ARTIST = new Property<Artist>("artist");
    public static final Property<Gallery> GALLERY = new Property<Gallery>("gallery");

    protected String name;
    protected ValueHolder artist;
    protected ValueHolder gallery;

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

    public Artist getArtist() {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "artist", true);
        } else if (this.artist == null) {
        	this.artist = new PersistentObjectHolder(this, "artist");
		}

        return (Artist) artist.getValue();
    }
    public void setArtist(Artist artist) {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "artist", true);
        } else if (this.artist == null) {
        	this.artist = new PersistentObjectHolder(this, "artist");
		}

        // note how we notify ObjectContext of change BEFORE the object is actually
        // changed... this is needed to take a valid current snapshot
        Object oldValue = this.artist.getValueDirectly();
        if (objectContext != null) {
        	objectContext.propertyChanged(this, "artist", oldValue, artist);
        }
        
        this.artist.setValue(artist);
    }

    public Gallery getGallery() {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "gallery", true);
        } else if (this.gallery == null) {
        	this.gallery = new PersistentObjectHolder(this, "gallery");
		}

        return (Gallery) gallery.getValue();
    }
    public void setGallery(Gallery gallery) {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "gallery", true);
        } else if (this.gallery == null) {
        	this.gallery = new PersistentObjectHolder(this, "gallery");
		}

        // note how we notify ObjectContext of change BEFORE the object is actually
        // changed... this is needed to take a valid current snapshot
        Object oldValue = this.gallery.getValueDirectly();
        if (objectContext != null) {
        	objectContext.propertyChanged(this, "gallery", oldValue, gallery);
        }
        
        this.gallery.setValue(gallery);
    }

}
