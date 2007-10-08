package org.objectstyle.art;

import org.objectstyle.cayenne.CayenneDataObject;

public class ArtistExhibit extends CayenneDataObject {

    public static final String TO_ARTIST_PROPERTY = "toArtist";
    public static final String TO_EXHIBIT_PROPERTY = "toExhibit";

    public static final String ARTIST_ID_PK_COLUMN = "ARTIST_ID";
    public static final String EXHIBIT_ID_PK_COLUMN = "EXHIBIT_ID";

    public void setToArtist(Artist toArtist) {
        setToOneTarget("toArtist", toArtist, true);
    }
    public Artist getToArtist() {
        return (Artist)readProperty("toArtist");
    } 
    
    
    public void setToExhibit(Exhibit toExhibit) {
        setToOneTarget("toExhibit", toExhibit, true);
    }
    public Exhibit getToExhibit() {
        return (Exhibit)readProperty("toExhibit");
    } 
    
    
}



