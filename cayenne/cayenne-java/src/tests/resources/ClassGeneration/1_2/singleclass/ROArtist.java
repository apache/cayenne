package org.objectstyle.art;

import java.sql.Date;
import java.util.List;

import org.objectstyle.cayenne.CayenneDataObject;

public class ROArtist extends CayenneDataObject {

    public static final String ARTIST_NAME_PROPERTY = "artistName";
    public static final String DATE_OF_BIRTH_PROPERTY = "dateOfBirth";
    public static final String PAINTING_ARRAY_PROPERTY = "paintingArray";

    public static final String ARTIST_ID_PK_COLUMN = "ARTIST_ID";

    public String getArtistName() {
        return (String)readProperty("artistName");
    }
    
    
    public Date getDateOfBirth() {
        return (Date)readProperty("dateOfBirth");
    }
    
    
    public void addToPaintingArray(Painting obj) {
        addToManyTarget("paintingArray", obj, true);
    }
    public void removeFromPaintingArray(Painting obj) {
        removeToManyTarget("paintingArray", obj, true);
    }
    public List getPaintingArray() {
        return (List)readProperty("paintingArray");
    }
    
    
}



