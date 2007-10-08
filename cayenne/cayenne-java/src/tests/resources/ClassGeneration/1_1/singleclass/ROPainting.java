package org.objectstyle.art;

public class ROPainting extends org.objectstyle.cayenne.CayenneDataObject {

    public static final String ESTIMATED_PRICE_PROPERTY = "estimatedPrice";
    public static final String PAINTING_TITLE_PROPERTY = "paintingTitle";
    public static final String TO_ARTIST_PROPERTY = "toArtist";

    public static final String PAINTING_ID_PK_COLUMN = "PAINTING_ID";

    public java.math.BigDecimal getEstimatedPrice() {
        return (java.math.BigDecimal)readProperty("estimatedPrice");
    }
    
    
    public String getPaintingTitle() {
        return (String)readProperty("paintingTitle");
    }
    
    
    public Artist getToArtist() {
        return (Artist)readProperty("toArtist");
    } 
    
    
}



