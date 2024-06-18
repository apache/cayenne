package org.apache.cayenne.lifecycle.db.auto;

import org.apache.cayenne.GenericPersistentObject;
import org.apache.cayenne.exp.property.PropertyFactory;
import org.apache.cayenne.exp.property.StringProperty;

/**
 * Class _UuidRoot1 was generated by Cayenne.
 * It is probably a good idea to avoid changing this class manually,
 * since it may be overwritten next time code is regenerated.
 * If you need to make any customizations, please use subclass.
 */
public abstract class _UuidRoot1 extends GenericPersistentObject {

    private static final long serialVersionUID = 1L; 

    public static final String ID_PK_COLUMN = "ID";

    public static final StringProperty<String> UUID = PropertyFactory.createString("uuid", String.class);

    public void setUuid(String uuid) {
        writeProperty("uuid", uuid);
    }
    public String getUuid() {
        return (String)readProperty("uuid");
    }

}
