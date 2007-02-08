package org.objectstyle.art;

import org.objectstyle.cayenne.CayenneDataObject;

public class BlobTest extends CayenneDataObject {

    public static final String BLOB_COL_PROPERTY = "blobCol";

    public static final String BLOB_TEST_ID_PK_COLUMN = "BLOB_TEST_ID";

    public void setBlobCol(byte[] blobCol) {
        writeProperty("blobCol", blobCol);
    }
    public byte[] getBlobCol() {
        return (byte[])readProperty("blobCol");
    }
    
    
}



