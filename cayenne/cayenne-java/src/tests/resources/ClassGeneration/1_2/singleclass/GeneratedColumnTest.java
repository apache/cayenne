package org.objectstyle.art;

import org.objectstyle.cayenne.CayenneDataObject;

public class GeneratedColumnTest extends CayenneDataObject {

    public static final String NAME_PROPERTY = "name";
    public static final String TO_DEP_PROPERTY = "toDep";

    public static final String GENERATED_COLUMN_PK_COLUMN = "GENERATED_COLUMN";

    public void setName(String name) {
        writeProperty("name", name);
    }
    public String getName() {
        return (String)readProperty("name");
    }
    
    
    public void setToDep(GeneratedColumnDep toDep) {
        setToOneTarget("toDep", toDep, true);
    }
    public GeneratedColumnDep getToDep() {
        return (GeneratedColumnDep)readProperty("toDep");
    } 
    
    
}



