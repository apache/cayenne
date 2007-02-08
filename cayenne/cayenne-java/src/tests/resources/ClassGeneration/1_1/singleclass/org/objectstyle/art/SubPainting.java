package org.objectstyle.art;

public class SubPainting extends org.objectstyle.cayenne.CayenneDataObject {

    public static final String PAINTING_TITLE_PROPERTY = "paintingTitle";

    public static final String PAINTING_ID_PK_COLUMN = "PAINTING_ID";

    public void setPaintingTitle(String paintingTitle) {
        writeProperty("paintingTitle", paintingTitle);
    }
    public String getPaintingTitle() {
        return (String)readProperty("paintingTitle");
    }
    
    
}



