package org.objectstyle.art;

public class BitTest extends org.objectstyle.cayenne.CayenneDataObject {

    public static final String BIT_COLUMN_PROPERTY = "bitColumn";

    public static final String ID_PK_COLUMN = "ID";

    public void setBitColumn(Boolean bitColumn) {
        writeProperty("bitColumn", bitColumn);
    }
    public Boolean getBitColumn() {
        return (Boolean)readProperty("bitColumn");
    }
    
    
}



