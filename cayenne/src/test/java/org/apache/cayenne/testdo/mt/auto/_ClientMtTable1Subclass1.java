package org.apache.cayenne.testdo.mt.auto;

import org.apache.cayenne.exp.property.PropertyFactory;
import org.apache.cayenne.exp.property.StringProperty;
import org.apache.cayenne.testdo.mt.ClientMtTable1;

/**
 * A generated persistent class mapped as "MtTable1Subclass1" Cayenne entity. It is a good idea to
 * avoid changing this class manually, since it will be overwritten next time code is
 * regenerated. If you need to make any customizations, put them in a subclass.
 */
public abstract class _ClientMtTable1Subclass1 extends ClientMtTable1 {

    public static final StringProperty<String> SUBCLASS1ATTRIBUTE1 = PropertyFactory.createString("subclass1Attribute1", String.class);

    protected String subclass1Attribute1;

    public String getSubclass1Attribute1() {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "subclass1Attribute1", false);
        }

        return subclass1Attribute1;
    }

    public void setSubclass1Attribute1(String subclass1Attribute1) {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "subclass1Attribute1", false);
            objectContext.propertyChanged(this, "subclass1Attribute1", this.subclass1Attribute1, subclass1Attribute1);
        }

        this.subclass1Attribute1 = subclass1Attribute1;
    }

}
