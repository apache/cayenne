package org.objectstyle.art;

import org.objectstyle.cayenne.CayenneDataObject;

public class BitNumberTest extends CayenneDataObject {

    public static final String BIT_COLUMN_PROPERTY = "bitColumn";

    public static final String ID_PK_COLUMN = "ID";

    public void setBitColumn(Integer bitColumn) {
        writeProperty("bitColumn", bitColumn);
    }
    public Integer getBitColumn() {
        return (Integer)readProperty("bitColumn");
    }
    
    
}



