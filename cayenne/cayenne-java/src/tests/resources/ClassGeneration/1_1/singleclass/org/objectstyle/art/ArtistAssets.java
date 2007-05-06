package org.objectstyle.art;

public class ArtistAssets extends org.objectstyle.cayenne.CayenneDataObject {

    public static final String ESTIMATED_PRICE_PROPERTY = "estimatedPrice";
    public static final String PAINTINGS_COUNT_PROPERTY = "paintingsCount";
    public static final String TO_ARTIST_PROPERTY = "toArtist";

    public static final String ARTIST_ID_PK_COLUMN = "ARTIST_ID";

    public void setEstimatedPrice(java.math.BigDecimal estimatedPrice) {
        writeProperty("estimatedPrice", estimatedPrice);
    }
    public java.math.BigDecimal getEstimatedPrice() {
        return (java.math.BigDecimal)readProperty("estimatedPrice");
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



