package org.objectstyle.art;

public class NoPkTest extends org.objectstyle.cayenne.CayenneDataObject {

    public static final String ATTRIBUTE1_PROPERTY = "attribute1";


    public void setAttribute1(Integer attribute1) {
        writeProperty("attribute1", attribute1);
    }
    public Integer getAttribute1() {
        return (Integer)readProperty("attribute1");
    }
    
    
}



