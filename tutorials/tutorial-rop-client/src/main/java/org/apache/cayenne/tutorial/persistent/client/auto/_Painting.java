package org.apache.cayenne.tutorial.persistent.client.auto;

import org.apache.cayenne.PersistentObject;
import org.apache.cayenne.ValueHolder;
import org.apache.cayenne.tutorial.persistent.client.Artist;
import org.apache.cayenne.tutorial.persistent.client.Gallery;

/**
 * A generated persistent class mapped as "Painting" Cayenne entity. It is a good idea to
 * avoid changing this class manually, since it will be overwritten next time code is
 * regenerated. If you need to make any customizations, put them in a subclass.
 */
public abstract class _Painting extends PersistentObject {

    public static final String NAME_PROPERTY = "name";
    public static final String ARTIST_PROPERTY = "artist";
    public static final String GALLERY_PROPERTY = "gallery";

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
        this.name = name;

        // notify objectContext about simple property change
        if(objectContext != null) {
            objectContext.propertyChanged(this, "name", oldValue, name);
        }
    }

    public Artist getArtist() {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "artist", true);
        }

        return (Artist) artist.getValue();
    }
    public void setArtist(Artist artist) {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "artist", true);
        }

        this.artist.setValue(artist);
    }

    public Gallery getGallery() {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "gallery", true);
        }

        return (Gallery) gallery.getValue();
    }
    public void setGallery(Gallery gallery) {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "gallery", true);
        }

        this.gallery.setValue(gallery);
    }

}
