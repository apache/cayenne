package org.objectstyle.art;

public class GeneratedColumnDep extends org.objectstyle.cayenne.CayenneDataObject {

    public static final String NAME_PROPERTY = "name";
    public static final String TO_MASTER_PROPERTY = "toMaster";

    public static final String GENERATED_COLUMN_FK_PK_COLUMN = "GENERATED_COLUMN_FK";

    public void setName(String name) {
        writeProperty("name", name);
    }
    public String getName() {
        return (String)readProperty("name");
    }
    
    
    public void setToMaster(GeneratedColumnTest toMaster) {
        setToOneTarget("toMaster", toMaster, true);
    }
    public GeneratedColumnTest getToMaster() {
        return (GeneratedColumnTest)readProperty("toMaster");
    } 
    
    
}



