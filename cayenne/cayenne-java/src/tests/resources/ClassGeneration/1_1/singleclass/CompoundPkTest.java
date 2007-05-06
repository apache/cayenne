package org.objectstyle.art;

import java.util.List;

public class CompoundPkTest extends org.objectstyle.cayenne.CayenneDataObject {

    public static final String KEY1_PROPERTY = "key1";
    public static final String KEY2_PROPERTY = "key2";
    public static final String NAME_PROPERTY = "name";
    public static final String COMPOUND_FK_ARRAY_PROPERTY = "compoundFkArray";

    public static final String KEY1_PK_COLUMN = "KEY1";
    public static final String KEY2_PK_COLUMN = "KEY2";

    public void setKey1(String key1) {
        writeProperty("key1", key1);
    }
    public String getKey1() {
        return (String)readProperty("key1");
    }
    
    
    public void setKey2(String key2) {
        writeProperty("key2", key2);
    }
    public String getKey2() {
        return (String)readProperty("key2");
    }
    
    
    public void setName(String name) {
        writeProperty("name", name);
    }
    public String getName() {
        return (String)readProperty("name");
    }
    
    
    public void addToCompoundFkArray(CompoundFkTest obj) {
        addToManyTarget("compoundFkArray", obj, true);
    }
    public void removeFromCompoundFkArray(CompoundFkTest obj) {
        removeToManyTarget("compoundFkArray", obj, true);
    }
    public List getCompoundFkArray() {
        return (List)readProperty("compoundFkArray");
    }
    
    
}



