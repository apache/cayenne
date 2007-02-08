package org.apache.art;

public class SmallintTest extends org.apache.cayenne.CayenneDataObject {

    public static final String SMALLINT_COL_PROPERTY = "smallintCol";

    public static final String ID_PK_COLUMN = "ID";

    public void setSmallintCol(Short smallintCol) {
        writeProperty("smallintCol", smallintCol);
    }
    public Short getSmallintCol() {
        return (Short)readProperty("smallintCol");
    }
    
    
}



