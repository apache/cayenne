package org.objectstyle.art;

public class BlobTest extends org.objectstyle.cayenne.CayenneDataObject {

    public static final String BLOB_COL_PROPERTY = "blobCol";

    public static final String BLOB_TEST_ID_PK_COLUMN = "BLOB_TEST_ID";

    public void setBlobCol(byte[] blobCol) {
        writeProperty("blobCol", blobCol);
    }
    public byte[] getBlobCol() {
        return (byte[])readProperty("blobCol");
    }
    
    
}



