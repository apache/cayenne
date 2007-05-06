package org.objectstyle.art;

import java.util.List;

public class GeneratedColumnCompMaster extends org.objectstyle.cayenne.CayenneDataObject {

    public static final String NAME_PROPERTY = "name";
    public static final String TO_DETAIL_PROPERTY = "toDetail";

    public static final String ID_PK_COLUMN = "ID";

    public void setName(String name) {
        writeProperty("name", name);
    }
    public String getName() {
        return (String)readProperty("name");
    }
    
    
    public void addToToDetail(GeneratedColumnCompKey obj) {
        addToManyTarget("toDetail", obj, true);
    }
    public void removeFromToDetail(GeneratedColumnCompKey obj) {
        removeToManyTarget("toDetail", obj, true);
    }
    public List getToDetail() {
        return (List)readProperty("toDetail");
    }
    
    
}



