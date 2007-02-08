package org.apache.art.auto;

import java.util.List;

import org.apache.art.MeaningfulPKDep;
import org.apache.cayenne.CayenneDataObject;

/** 
 * Class _MeaningfulPKTest1 was generated by Cayenne.
 * It is probably a good idea to avoid changing this class manually, 
 * since it may be overwritten next time code is regenerated. 
 * If you need to make any customizations, please use subclass. 
 */
public class _MeaningfulPKTest1 extends CayenneDataObject {

    public static final String DESCR_PROPERTY = "descr";
    public static final String PK_ATTRIBUTE_PROPERTY = "pkAttribute";
    public static final String MEANINGFUL_PKDEP_ARRAY_PROPERTY = "meaningfulPKDepArray";

    public static final String PK_ATTRIBUTE_PK_COLUMN = "PK_ATTRIBUTE";

    public void setDescr(String descr) {
        writeProperty("descr", descr);
    }
    public String getDescr() {
        return (String)readProperty("descr");
    }
    
    
    public void setPkAttribute(Integer pkAttribute) {
        writeProperty("pkAttribute", pkAttribute);
    }
    public Integer getPkAttribute() {
        return (Integer)readProperty("pkAttribute");
    }
    
    
    public void addToMeaningfulPKDepArray(MeaningfulPKDep obj) {
        addToManyTarget("meaningfulPKDepArray", obj, true);
    }
    public void removeFromMeaningfulPKDepArray(MeaningfulPKDep obj) {
        removeToManyTarget("meaningfulPKDepArray", obj, true);
    }
    public List getMeaningfulPKDepArray() {
        return (List)readProperty("meaningfulPKDepArray");
    }
    
    
}
