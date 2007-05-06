package org.objectstyle.art;

public class CharFkTest extends org.objectstyle.cayenne.CayenneDataObject {

    public static final String NAME_PROPERTY = "name";
    public static final String TO_CHAR_PK_PROPERTY = "toCharPK";

    public static final String PK_PK_COLUMN = "PK";

    public void setName(String name) {
        writeProperty("name", name);
    }
    public String getName() {
        return (String)readProperty("name");
    }
    
    
    public void setToCharPK(CharPkTest toCharPK) {
        setToOneTarget("toCharPK", toCharPK, true);
    }
    public CharPkTest getToCharPK() {
        return (CharPkTest)readProperty("toCharPK");
    } 
    
    
}



