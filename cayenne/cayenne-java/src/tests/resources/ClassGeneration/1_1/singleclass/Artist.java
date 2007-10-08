package org.objectstyle.art;

import java.util.List;

public class Artist extends org.objectstyle.cayenne.CayenneDataObject {

    public static final String ARTIST_NAME_PROPERTY = "artistName";
    public static final String DATE_OF_BIRTH_PROPERTY = "dateOfBirth";
    public static final String ARTIST_EXHIBIT_ARRAY_PROPERTY = "artistExhibitArray";
    public static final String GROUP_ARRAY_PROPERTY = "groupArray";
    public static final String PAINTING_ARRAY_PROPERTY = "paintingArray";

    public static final String ARTIST_ID_PK_COLUMN = "ARTIST_ID";

    public void setArtistName(String artistName) {
        writeProperty("artistName", artistName);
    }
    public String getArtistName() {
        return (String)readProperty("artistName");
    }
    
    
    public void setDateOfBirth(java.util.Date dateOfBirth) {
        writeProperty("dateOfBirth", dateOfBirth);
    }
    public java.util.Date getDateOfBirth() {
        return (java.util.Date)readProperty("dateOfBirth");
    }
    
    
    public void addToArtistExhibitArray(ArtistExhibit obj) {
        addToManyTarget("artistExhibitArray", obj, true);
    }
    public void removeFromArtistExhibitArray(ArtistExhibit obj) {
        removeToManyTarget("artistExhibitArray", obj, true);
    }
    public List getArtistExhibitArray() {
        return (List)readProperty("artistExhibitArray");
    }
    
    
    public void addToGroupArray(ArtGroup obj) {
        addToManyTarget("groupArray", obj, true);
    }
    public void removeFromGroupArray(ArtGroup obj) {
        removeToManyTarget("groupArray", obj, true);
    }
    public List getGroupArray() {
        return (List)readProperty("groupArray");
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



