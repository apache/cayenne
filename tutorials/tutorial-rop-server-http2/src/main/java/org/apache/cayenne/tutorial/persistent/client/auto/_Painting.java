package org.apache.cayenne.tutorial.persistent.client.auto;

import org.apache.cayenne.PersistentObject;
import org.apache.cayenne.ValueHolder;
import org.apache.cayenne.exp.property.EntityProperty;
import org.apache.cayenne.exp.property.PropertyFactory;
import org.apache.cayenne.exp.property.StringProperty;
import org.apache.cayenne.tutorial.persistent.client.Artist;
import org.apache.cayenne.tutorial.persistent.client.Gallery;
import org.apache.cayenne.util.PersistentObjectHolder;

/**
 * A generated persistent class mapped as "Painting" Cayenne entity. It is a good idea to
 * avoid changing this class manually, since it will be overwritten next time code is
 * regenerated. If you need to make any customizations, put them in a subclass.
 */
public abstract class _Painting extends PersistentObject {

    public static final StringProperty<String> NAME = PropertyFactory.createString("name", String.class);
    public static final EntityProperty<Artist> ARTIST = PropertyFactory.createEntity("artist", Artist.class);
    public static final EntityProperty<Gallery> GALLERY = PropertyFactory.createEntity("gallery", Gallery.class);

    protected String name;
    protected ValueHolder<Artist> artist;
    protected ValueHolder<Gallery> gallery;

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

    public Artist getArtist() {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "artist", true);
        } else if (this.artist == null) {
        	this.artist = new PersistentObjectHolder<>(this, "artist");
		}

        return artist.getValue();
    }

    public void setArtist(Artist artist) {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "artist", true);
        } else if (this.artist == null) {
        	this.artist = new PersistentObjectHolder<>(this, "artist");
		}

        this.artist.setValue(artist);
    }

    public Gallery getGallery() {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "gallery", true);
        } else if (this.gallery == null) {
        	this.gallery = new PersistentObjectHolder<>(this, "gallery");
		}

        return gallery.getValue();
    }

    public void setGallery(Gallery gallery) {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "gallery", true);
        } else if (this.gallery == null) {
        	this.gallery = new PersistentObjectHolder<>(this, "gallery");
		}

        this.gallery.setValue(gallery);
    }

}
