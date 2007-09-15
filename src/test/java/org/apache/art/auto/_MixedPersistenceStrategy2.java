package org.apache.art.auto;

/** Class _MixedPersistenceStrategy2 was generated by Cayenne.
  * It is probably a good idea to avoid changing this class manually, 
  * since it may be overwritten next time code is regenerated. 
  * If you need to make any customizations, please use subclass. 
  */
public abstract class _MixedPersistenceStrategy2 extends org.apache.cayenne.CayenneDataObject {

    public static final String NAME_PROPERTY = "name";
    public static final String MASTER_PROPERTY = "master";

    public static final String ID_PK_COLUMN = "ID";

    public void setName(String name) {
        writeProperty("name", name);
    }
    public String getName() {
        return (String)readProperty("name");
    }
    
    
    public void setMaster(org.apache.art.MixedPersistenceStrategy master) {
        setToOneTarget("master", master, true);
    }

    public org.apache.art.MixedPersistenceStrategy getMaster() {
        return (org.apache.art.MixedPersistenceStrategy)readProperty("master");
    } 
    
    
}
