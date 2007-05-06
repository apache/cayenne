package org.objectstyle.art;

public class TinyintTest extends org.objectstyle.cayenne.CayenneDataObject {

    public static final String TINYINT_COL_PROPERTY = "tinyintCol";

    public static final String ID_PK_COLUMN = "ID";

    public void setTinyintCol(Byte tinyintCol) {
        writeProperty("tinyintCol", tinyintCol);
    }
    public Byte getTinyintCol() {
        return (Byte)readProperty("tinyintCol");
    }
    
    
}



