package org.objectstyle.art;

import java.util.List;

import org.objectstyle.cayenne.CayenneDataObject;

public class BinaryPKTest1 extends CayenneDataObject {

    public static final String NAME_PROPERTY = "name";
    public static final String BINARY_PKDETAILS_PROPERTY = "binaryPKDetails";

    public static final String BIN_ID_PK_COLUMN = "BIN_ID";

    public void setName(String name) {
        writeProperty("name", name);
    }
    public String getName() {
        return (String)readProperty("name");
    }
    
    
    public void addToBinaryPKDetails(BinaryPKTest2 obj) {
        addToManyTarget("binaryPKDetails", obj, true);
    }
    public void removeFromBinaryPKDetails(BinaryPKTest2 obj) {
        removeToManyTarget("binaryPKDetails", obj, true);
    }
    public List getBinaryPKDetails() {
        return (List)readProperty("binaryPKDetails");
    }
    
    
}



