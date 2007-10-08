package org.objectstyle.art;

import org.objectstyle.cayenne.CayenneDataObject;

public class CompoundFkTest extends CayenneDataObject {

    public static final String NAME_PROPERTY = "name";
    public static final String TO_COMPOUND_PK_PROPERTY = "toCompoundPk";

    public static final String PKEY_PK_COLUMN = "PKEY";

    public void setName(String name) {
        writeProperty("name", name);
    }
    public String getName() {
        return (String)readProperty("name");
    }
    
    
    public void setToCompoundPk(CompoundPkTest toCompoundPk) {
        setToOneTarget("toCompoundPk", toCompoundPk, true);
    }
    public CompoundPkTest getToCompoundPk() {
        return (CompoundPkTest)readProperty("toCompoundPk");
    } 
    
    
}



