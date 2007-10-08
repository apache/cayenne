package org.objectstyle.art;

import org.objectstyle.cayenne.CayenneDataObject;

public class MeaningfulPKDep extends CayenneDataObject {

    public static final String DESCR_PROPERTY = "descr";
    public static final String TO_MEANINGFUL_PK_PROPERTY = "toMeaningfulPK";

    public static final String PK_ATTRIBUTE_PK_COLUMN = "PK_ATTRIBUTE";

    public void setDescr(String descr) {
        writeProperty("descr", descr);
    }
    public String getDescr() {
        return (String)readProperty("descr");
    }
    
    
    public void setToMeaningfulPK(MeaningfulPKTest1 toMeaningfulPK) {
        setToOneTarget("toMeaningfulPK", toMeaningfulPK, true);
    }
    public MeaningfulPKTest1 getToMeaningfulPK() {
        return (MeaningfulPKTest1)readProperty("toMeaningfulPK");
    } 
    
    
}



