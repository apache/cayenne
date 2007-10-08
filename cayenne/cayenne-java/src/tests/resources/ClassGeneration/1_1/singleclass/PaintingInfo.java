package org.objectstyle.art;

public class PaintingInfo extends org.objectstyle.cayenne.CayenneDataObject {

    public static final String IMAGE_BLOB_PROPERTY = "imageBlob";
    public static final String TEXT_REVIEW_PROPERTY = "textReview";
    public static final String PAINTING_PROPERTY = "painting";

    public static final String PAINTING_ID_PK_COLUMN = "PAINTING_ID";

    public void setImageBlob(byte[] imageBlob) {
        writeProperty("imageBlob", imageBlob);
    }
    public byte[] getImageBlob() {
        return (byte[])readProperty("imageBlob");
    }
    
    
    public void setTextReview(String textReview) {
        writeProperty("textReview", textReview);
    }
    public String getTextReview() {
        return (String)readProperty("textReview");
    }
    
    
    public void setPainting(Painting painting) {
        setToOneTarget("painting", painting, true);
    }
    public Painting getPainting() {
        return (Painting)readProperty("painting");
    } 
    
    
}



