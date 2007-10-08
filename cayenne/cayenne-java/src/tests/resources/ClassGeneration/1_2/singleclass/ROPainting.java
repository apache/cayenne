package org.objectstyle.art;

import java.math.BigDecimal;

import org.objectstyle.cayenne.CayenneDataObject;

public class ROPainting extends CayenneDataObject {

    public static final String ESTIMATED_PRICE_PROPERTY = "estimatedPrice";
    public static final String PAINTING_TITLE_PROPERTY = "paintingTitle";
    public static final String TO_ARTIST_PROPERTY = "toArtist";

    public static final String PAINTING_ID_PK_COLUMN = "PAINTING_ID";

    public BigDecimal getEstimatedPrice() {
        return (BigDecimal)readProperty("estimatedPrice");
    }
    
    
    public String getPaintingTitle() {
        return (String)readProperty("paintingTitle");
    }
    
    
    public Artist getToArtist() {
        return (Artist)readProperty("toArtist");
    } 
    
    
}



