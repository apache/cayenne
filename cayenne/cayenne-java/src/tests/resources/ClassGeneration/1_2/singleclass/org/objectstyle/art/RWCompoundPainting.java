package org.objectstyle.art;

import java.math.BigDecimal;

import org.objectstyle.cayenne.CayenneDataObject;

public class RWCompoundPainting extends CayenneDataObject {

    public static final String ESTIMATED_PRICE_PROPERTY = "estimatedPrice";
    public static final String PAINTING_TITLE_PROPERTY = "paintingTitle";
    public static final String TEXT_REVIEW_PROPERTY = "textReview";

    public static final String PAINTING_ID_PK_COLUMN = "PAINTING_ID";

    public void setEstimatedPrice(BigDecimal estimatedPrice) {
        writeProperty("estimatedPrice", estimatedPrice);
    }
    public BigDecimal getEstimatedPrice() {
        return (BigDecimal)readProperty("estimatedPrice");
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



