package cayenne.tutorial.client.auto;

import java.util.Date;
import java.util.List;

import org.apache.cayenne.PersistentObject;

import cayenne.tutorial.client.Painting;

/**
 * A generated persistent class mapped as "Artist" Cayenne entity. It is a good idea to
 * avoid changing this class manually, since it will be overwritten next time code is
 * regenerated. If you need to make any customizations, put them in a subclass.
 */
public class _Artist extends PersistentObject {

    public static final String DATE_OF_BIRTH_PROPERTY = "dateOfBirth";
    public static final String NAME_PROPERTY = "name";
    public static final String PAINTINGS_PROPERTY = "paintings";

    protected Date dateOfBirth;
    protected String name;
    protected List paintings;

    public Date getDateOfBirth() {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "dateOfBirth");
        }
        
        return dateOfBirth;
    }
    public void setDateOfBirth(Date dateOfBirth) {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "dateOfBirth");
        }
        
        Object oldValue = this.dateOfBirth;
        this.dateOfBirth = dateOfBirth;
        
        // notify objectContext about simple property change
        if(objectContext != null) {
            objectContext.propertyChanged(this, "dateOfBirth", oldValue, dateOfBirth);
        }
    }
    
    
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
    
    
    public List getPaintings() {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "paintings");
        }
        
        return paintings;
    }
    public void addToPaintings(Painting object) {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "paintings");
        }
        
        this.paintings.add(object);
    }
    public void removeFromPaintings(Painting object) {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "paintings");
        }
        
        this.paintings.remove(object);
    }
    
}
