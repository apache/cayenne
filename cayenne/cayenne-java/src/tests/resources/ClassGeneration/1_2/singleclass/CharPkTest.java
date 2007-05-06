package org.objectstyle.art;

import java.util.List;

import org.objectstyle.cayenne.CayenneDataObject;

public class CharPkTest extends CayenneDataObject {

    public static final String OTHER_COL_PROPERTY = "otherCol";
    public static final String PK_COL_PROPERTY = "pkCol";
    public static final String CHAR_FKS_PROPERTY = "charFKs";

    public static final String PK_COL_PK_COLUMN = "PK_COL";

    public void setOtherCol(String otherCol) {
        writeProperty("otherCol", otherCol);
    }
    public String getOtherCol() {
        return (String)readProperty("otherCol");
    }
    
    
    public void setPkCol(String pkCol) {
        writeProperty("pkCol", pkCol);
    }
    public String getPkCol() {
        return (String)readProperty("pkCol");
    }
    
    
    public void addToCharFKs(CharFkTest obj) {
        addToManyTarget("charFKs", obj, true);
    }
    public void removeFromCharFKs(CharFkTest obj) {
        removeToManyTarget("charFKs", obj, true);
    }
    public List getCharFKs() {
        return (List)readProperty("charFKs");
    }
    
    
}



