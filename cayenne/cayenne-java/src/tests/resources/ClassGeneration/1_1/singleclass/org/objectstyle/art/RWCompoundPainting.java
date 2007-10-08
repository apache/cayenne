package org.objectstyle.art;

public class RWCompoundPainting extends org.objectstyle.cayenne.CayenneDataObject {

    public static final String ESTIMATED_PRICE_PROPERTY = "estimatedPrice";
    public static final String PAINTING_TITLE_PROPERTY = "paintingTitle";
    public static final String TEXT_REVIEW_PROPERTY = "textReview";

    public static final String PAINTING_ID_PK_COLUMN = "PAINTING_ID";

    public void setEstimatedPrice(java.math.BigDecimal estimatedPrice) {
        writeProperty("estimatedPrice", estimatedPrice);
    }
    public java.math.BigDecimal getEstimatedPrice() {
        return (java.math.BigDecimal)readProperty("estimatedPrice");
    }
    
    
    public void setPaintingTitle(String paintingTitle) {
        writeProperty("paintingTitle", paintingTitle);
    }
    public String getPaintingTitle() {
        return (String)readProperty("paintingTitle");
    }
    
    
    public void setTextReview(String textReview) {
        writeProperty("textReview", textReview);
    }
    public String getTextReview() {
        return (String)readProperty("textReview");
    }
    
    
}



