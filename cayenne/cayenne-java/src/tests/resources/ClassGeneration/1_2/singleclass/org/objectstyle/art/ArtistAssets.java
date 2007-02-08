package org.objectstyle.art;

import java.math.BigDecimal;

import org.objectstyle.cayenne.CayenneDataObject;

public class ArtistAssets extends CayenneDataObject {

    public static final String ESTIMATED_PRICE_PROPERTY = "estimatedPrice";
    public static final String PAINTINGS_COUNT_PROPERTY = "paintingsCount";
    public static final String TO_ARTIST_PROPERTY = "toArtist";

    public static final String ARTIST_ID_PK_COLUMN = "ARTIST_ID";

    public void setEstimatedPrice(BigDecimal estimatedPrice) {
        writeProperty("estimatedPrice", estimatedPrice);
    }
    public BigDecimal getEstimatedPrice() {
        return (BigDecimal)readProperty("estimatedPrice");
    }
    
    
    public void setPaintingsCount(Integer paintingsCount) {
        writeProperty("paintingsCount", paintingsCount);
    }
    public Integer getPaintingsCount() {
        return (Integer)readProperty("paintingsCount");
    }
    
    
    public void setToArtist(Artist toArtist) {
        setToOneTarget("toArtist", toArtist, true);
    }
    public Artist getToArtist() {
        return (Artist)readProperty("toArtist");
    } 
    
    
}



