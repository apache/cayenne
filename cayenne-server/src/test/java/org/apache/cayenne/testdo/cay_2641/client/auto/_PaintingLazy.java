package org.apache.cayenne.testdo.cay_2641.client.auto;

import org.apache.cayenne.Fault;
import org.apache.cayenne.PersistentObject;
import org.apache.cayenne.ValueHolder;
import org.apache.cayenne.exp.property.EntityProperty;
import org.apache.cayenne.exp.property.PropertyFactory;
import org.apache.cayenne.exp.property.StringProperty;
import org.apache.cayenne.testdo.cay_2641.client.ArtistLazy;
import org.apache.cayenne.util.PersistentObjectHolder;

/**
 * A generated persistent class mapped as "PaintingLazy" Cayenne entity. It is a good idea to
 * avoid changing this class manually, since it will be overwritten next time code is
 * regenerated. If you need to make any customizations, put them in a subclass.
 */
public abstract class _PaintingLazy extends PersistentObject {

    public static final StringProperty<String> NAME = PropertyFactory.createString("name", String.class);
    public static final EntityProperty<ArtistLazy> ARTIST = PropertyFactory.createEntity("artist", ArtistLazy.class);

    protected Object name;
    protected ValueHolder<ArtistLazy> artist;

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

    public ArtistLazy getArtist() {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "artist", true);
        } else if (this.artist == null) {
        	this.artist = new PersistentObjectHolder<>(this, "artist");
		}

        return artist.getValue();
    }

    public void setArtist(ArtistLazy artist) {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "artist", true);
        } else if (this.artist == null) {
        	this.artist = new PersistentObjectHolder<>(this, "artist");
		}

        this.artist.setValue(artist);
    }

}
