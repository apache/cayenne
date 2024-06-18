package org.apache.cayenne.testdo.mt.auto;

import org.apache.cayenne.exp.property.PropertyFactory;
import org.apache.cayenne.exp.property.StringProperty;
import org.apache.cayenne.testdo.mt.ClientMtTable1;

/**
 * A generated persistent class mapped as "MtTable1Subclass2" Cayenne entity. It is a good idea to
 * avoid changing this class manually, since it will be overwritten next time code is
 * regenerated. If you need to make any customizations, put them in a subclass.
 */
public abstract class _ClientMtTable1Subclass2 extends ClientMtTable1 {

    public static final StringProperty<String> SUBCLASS2ATTRIBUTE1 = PropertyFactory.createString("subclass2Attribute1", String.class);

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
            objectContext.propertyChanged(this, "subclass2Attribute1", this.subclass2Attribute1, subclass2Attribute1);
        }

        this.subclass2Attribute1 = subclass2Attribute1;
    }

}
