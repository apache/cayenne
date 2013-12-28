package org.apache.cayenne.testdo.mt.auto;

import org.apache.cayenne.exp.Property;
import org.apache.cayenne.testdo.mt.ClientMtTable1;

/**
 * A generated persistent class mapped as "MtTable1Subclass2" Cayenne entity. It is a good idea to
 * avoid changing this class manually, since it will be overwritten next time code is
 * regenerated. If you need to make any customizations, put them in a subclass.
 */
public abstract class _ClientMtTable1Subclass2 extends ClientMtTable1 {

    @Deprecated
    public static final String SUBCLASS2ATTRIBUTE1_PROPERTY = "subclass2Attribute1";

    public static final Property<String> SUBCLASS2ATTRIBUTE1 = new Property<String>("subclass2Attribute1");

    protected String subclass2Attribute1;

    public String getSubclass2Attribute1() {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "subclass2Attribute1", false);
        }

        return subclass2Attribute1;
    }
    public void setSubclass2Attribute1(String subclass2Attribute1) {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "subclass2Attribute1", false);
        }

        Object oldValue = this.subclass2Attribute1;
        // notify objectContext about simple property change
        if(objectContext != null) {
            objectContext.propertyChanged(this, "subclass2Attribute1", oldValue, subclass2Attribute1);
        }
        
        this.subclass2Attribute1 = subclass2Attribute1;
    }

}
