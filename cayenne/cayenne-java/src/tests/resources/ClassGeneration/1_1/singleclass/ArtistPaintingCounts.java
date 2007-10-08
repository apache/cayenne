package org.objectstyle.art;

public class ArtistPaintingCounts extends org.objectstyle.cayenne.CayenneDataObject {

    public static final String PAINTINGS_COUNT_PROPERTY = "paintingsCount";

    public static final String ARTIST_ID_PK_COLUMN = "ARTIST_ID";

    public void setPaintingsCount(Integer paintingsCount) {
        writeProperty("paintingsCount", paintingsCount);
    }
    public Integer getPaintingsCount() {
        return (Integer)readProperty("paintingsCount");
    }
    
    
}



