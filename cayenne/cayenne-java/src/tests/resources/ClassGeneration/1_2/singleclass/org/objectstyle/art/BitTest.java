package org.objectstyle.art;

import org.objectstyle.cayenne.CayenneDataObject;

public class BitTest extends CayenneDataObject {

    public static final String BIT_COLUMN_PROPERTY = "bitColumn";

    public static final String ID_PK_COLUMN = "ID";

    public void setBitColumn(Boolean bitColumn) {
        writeProperty("bitColumn", bitColumn);
    }
    public Boolean getBitColumn() {
        return (Boolean)readProperty("bitColumn");
    }
    
    
}



