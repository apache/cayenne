package cayenne.tutorial.client.auto;

import org.apache.cayenne.PersistentObject;
import org.apache.cayenne.ValueHolder;

import cayenne.tutorial.client.Artist;
import cayenne.tutorial.client.Gallery;

/**
 * A generated persistent class mapped as "Painting" Cayenne entity. It is a good idea to
 * avoid changing this class manually, since it will be overwritten next time code is
 * regenerated. If you need to make any customizations, put them in a subclass.
 */
public class _Painting extends PersistentObject {

    public static final String NAME_PROPERTY = "name";
    public static final String ARTIST_PROPERTY = "artist";
    public static final String GALLERY_PROPERTY = "gallery";

    protected String name;
    protected ValueHolder artist;
    protected ValueHolder gallery;

    public String getName() {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "name");
        }
        
        return name;
    }
    public void setName(String name) {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "name");
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
            objectContext.prepareForAccess(this, "artist");
        }
        
        return (Artist) artist.getValue();
    }
    public void setArtist(Artist artist) {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "artist");
        }
        
        this.artist.setValue(artist);
    }
    
    public Gallery getGallery() {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "gallery");
        }
        
        return (Gallery) gallery.getValue();
    }
    public void setGallery(Gallery gallery) {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "gallery");
        }
        
        this.gallery.setValue(gallery);
    }
    
}
