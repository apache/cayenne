package org.objectstyle.art;

public class CompoundPainting extends org.objectstyle.cayenne.CayenneDataObject {

    public static final String ARTIST_NAME_PROPERTY = "artistName";
    public static final String ESTIMATED_PRICE_PROPERTY = "estimatedPrice";
    public static final String GALLERY_NAME_PROPERTY = "galleryName";
    public static final String PAINTING_TITLE_PROPERTY = "paintingTitle";
    public static final String TEXT_REVIEW_PROPERTY = "textReview";
    public static final String TO_ARTIST_PROPERTY = "toArtist";
    public static final String TO_GALLERY_PROPERTY = "toGallery";
    public static final String TO_PAINTING_INFO_PROPERTY = "toPaintingInfo";

    public static final String PAINTING_ID_PK_COLUMN = "PAINTING_ID";

    public String getArtistName() {
        return (String)readProperty("artistName");
    }
    
    
    public java.math.BigDecimal getEstimatedPrice() {
        return (java.math.BigDecimal)readProperty("estimatedPrice");
    }
    
    
    public String getGalleryName() {
        return (String)readProperty("galleryName");
    }
    
    
    public String getPaintingTitle() {
        return (String)readProperty("paintingTitle");
    }
    
    
    public String getTextReview() {
        return (String)readProperty("textReview");
    }
    
    
    public Artist getToArtist() {
        return (Artist)readProperty("toArtist");
    } 
    
    
    public Gallery getToGallery() {
        return (Gallery)readProperty("toGallery");
    } 
    
    
    public PaintingInfo getToPaintingInfo() {
        return (PaintingInfo)readProperty("toPaintingInfo");
    } 
    
    
}



