package org.apache.art;

public class ClobTest extends org.apache.cayenne.CayenneDataObject {

    public static final String CLOB_COL_PROPERTY = "clobCol";

    public static final String CLOB_TEST_ID_PK_COLUMN = "CLOB_TEST_ID";

    public void setClobCol(String clobCol) {
        writeProperty("clobCol", clobCol);
    }
    public String getClobCol() {
        return (String)readProperty("clobCol");
    }
    
    
}



